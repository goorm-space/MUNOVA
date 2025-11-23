package com.space.munova.product.application;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.product.application.dto.cart.*;
import com.space.munova.product.application.exception.CartException;
import com.space.munova.product.domain.Cart;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.product.domain.Repository.CartRepository;
import com.space.munova.recommend.infra.RedisStreamProducer;
import com.space.munova.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

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

    private void upsertUserAction(List<Long> cartIds) {
        List<Long> productIdsByCartIds = cartRepository.findProductIdsByCartIds(cartIds);
        for(Long productId:productIdsByCartIds){
            recommendService.updateUserAction(productId,0,null,false,null);
        }
    }


    public PagingResponse<FindCartInfoResponseDto> findCartItemByMember(Pageable pageable, Long memberId) {


        ///  해당 페이지에 보여줄 상품디테일 아이디 (리밋 오프셋을통해 가져올 정보를 확인)
        Page<Long> detailIdsPage = cartRepository.findDistinctDetailIdsByMemberId(memberId, pageable);

        if (detailIdsPage.isEmpty()) {
            return PagingResponse.from(Page.empty());
        }
        List<Long> detailIds = detailIdsPage.getContent();

        /// 가져올 아이디 정보들을  가지고 다시 조회. -> 리밋 오프셋으로 상품정보가 한 페이지 안에 하나의 상품정보가 다 못담겨질수있기때문에
        ///  detailIdsPage 과 나눠서 다시조회.
        List<ProductInfoForCartDto> productInfoList = cartRepository.findCartItemInfoByDetailIds(detailIds);

        Map<Long, List<ProductInfoForCartDto>> groupedByDetail =
                productInfoList.stream()
                        .collect(Collectors.groupingBy(
                                ProductInfoForCartDto::detailId,
                                LinkedHashMap::new, // 순서 보장
                                Collectors.toList()
                        ));

        List<FindCartInfoResponseDto> content = detailIds.stream()
                .map(groupedByDetail::get)
                .map(FindCartInfoResponseDto::from)
                .collect(Collectors.toList());

        Page<FindCartInfoResponseDto> resultPage =
                new PageImpl<>(content, detailIdsPage.getPageable(), detailIdsPage.getTotalElements());

        return PagingResponse.from(resultPage);
    }

    @Transactional
    public void deleteByOrderItemsAndMemberId(List<OrderItem> orderItems, Long memberId) {
        List<Long> productDetailIds = orderItems.stream()
                .map(orderItem -> orderItem.getProductDetail().getId())
                .toList();

        cartRepository.deleteByProductDetailIdsAndMemberId(productDetailIds,memberId);
    }


}
