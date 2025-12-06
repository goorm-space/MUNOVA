package com.space.munova.product.application.cart.command;

import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.product.application.cart.command.dto.AddCartItemRequestDto;
import com.space.munova.product.application.cart.command.dto.UpdateCartRequestDto;
import com.space.munova.product.application.product.command.ProductDetailService;
import com.space.munova.product.application.cart.command.exception.CartException;
import com.space.munova.product.domain.Cart;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.product.domain.Repository.CartRepository;
import com.space.munova.recommend.infra.RedisStreamProducer;
import com.space.munova.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartCommandService {

    private final CartRepository cartRepository;
    private final MemberRepository memberRepository;
    private final ProductDetailService productDetailService;
    private final RecommendService recommendService;
    private final RedisStreamProducer logProducer;

    @Transactional(readOnly = false)
    public void deleteByProductDetailIds(List<Long> productDetailIds) {
        cartRepository.deleteByProductDetailIds(productDetailIds);
    }

    ///  카트 생성 메서드
    @Transactional(readOnly = false)
    public void addCartItem(AddCartItemRequestDto reqDto, Long memberId) {


        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberException::notFoundException);

        ProductDetail productDetail = productDetailService.findById((reqDto.productDetailId()));

        ///  상품 디테일 수량 및 제거여부 검증
        productDetail.validAddToCart(reqDto.quantity());


        ///  사용자의 장바구니에 상품디테일이 있는지 확인.
        boolean isExist = cartRepository.existsByMemberIdAndProductDetailId(memberId, productDetail.getId());

        if(isExist) { ///  있으면 수량확인후 업데이트

            Cart cart = cartRepository.findByProductDetailIdAndMemberId(productDetail.getId(), memberId)
                    .orElseThrow(CartException::badRequestCartException);
            cart.updateQuantity(reqDto.quantity());

        } else { /// 없으면 저장.

            Cart cart = Cart.createDefaultCart(member, productDetail, reqDto.quantity());
            cartRepository.save(cart);
        }

        Long productId=productDetailService.findProductIdByDetailId(reqDto.productDetailId());
        Map<String, Object> logData = Map.of(
                "event_type", "product_add_cart",
                "service", "product",
                "member_id", memberId,
                "data", Map.of(
                        "product_id", productId,
                        "quantity", productDetail.getQuantity()
                )
        );
        logProducer.sendLogAsync(RedisStreamProducer.StreamType.PRODUCT, logData);
    }


    @Transactional(readOnly = false)
    public void updateCartByMemeber(UpdateCartRequestDto reqDto, Long memberId) {

        Cart cartItem = cartRepository.findByIdAndMemberIdAndIsDeletedFalse(reqDto.cartId(), memberId).orElseThrow(CartException::badRequestCartException);
        ProductDetail productDetail = productDetailService.findById(reqDto.detailId());
        /// 더티체킹으로 카트 아이템 업데이트
        cartItem.updateCart(productDetail, reqDto.quantity());
    }

    /// 유저의 장바구니 카트 상품제거
    @Transactional(readOnly = false)
    public void deleteByCartIds(List<Long> cartIds,  Long memberId) {

        upsertUserAction(cartIds);
        cartRepository.deleteByCartIdsAndMemberId(cartIds,memberId);
    }


    @Transactional(readOnly = false)
    public void deleteByOrderItemsAndMemberId(List<OrderItem> orderItems, Long memberId) {
        List<Long> productDetailIds = orderItems.stream()
                .map(orderItem -> orderItem.getProductDetail().getId())
                .toList();

        cartRepository.deleteByProductDetailIdsAndMemberId(productDetailIds,memberId);
    }


    private void upsertUserAction(List<Long> cartIds) {
        List<Long> productIdsByCartIds = cartRepository.findProductIdsByCartIds(cartIds);
        for(Long productId:productIdsByCartIds){
            recommendService.updateUserAction(productId,0,null,false,null);
        }
    }

}
