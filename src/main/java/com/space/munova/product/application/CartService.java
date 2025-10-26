package com.space.munova.product.application;

import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.application.dto.cart.*;
import com.space.munova.product.application.exception.CartException;
import com.space.munova.product.domain.Cart;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.product.domain.Repository.CartRepository;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final MemberRepository memberRepository;
    private final ProductDetailService productDetailService;

    @Transactional(readOnly = false)
    public void deleteByProductDetailIds(List<Long> productDetailIds) {
        cartRepository.deleteByProductDetailIds(productDetailIds);
    }

    ///  카트 생성 메서드
    @Transactional(readOnly = false)
    public void addCartItem(AddCartItemRequestDto reqDto) {

        Long memberId = JwtHelper.getMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberException::notFoundException);

        ProductDetail productDetail = productDetailService.findById((reqDto.productDetailId()));

        ///  상품 디테일 수량 및 제거여부 검증
        validProductDetail(reqDto, productDetail);

        /// 멤버의 장바구니에 담긴상품일경우 업데이트
        /// 담기지 않은 상품일경우 저장.
        upsertCart(reqDto, memberId, productDetail, member);
    }



    /// 유저의 장바구니 카트 상품제거
    @Transactional(readOnly = false)
    public void deleteByCartIds(List<Long> cartIds) {

        Long memberId = JwtHelper.getMemberId();
        cartRepository.deleteByCartIdsAndMemberId(cartIds,memberId);
    }


    /// 장바구니 추가 상품 검증로직
    private void validProductDetail(AddCartItemRequestDto reqDto, ProductDetail productDetail) {
        if(productDetail.getQuantity() < reqDto.quantity()) {
            throw CartException.badRequestCartException("상품의 수량을 초과하여 상품을 담을 수 없습니다.");
        }
        if(productDetail.isDeleted()) {
            throw CartException.badRequestCartException("제거된 상품은 장바구니에 추가할 수 없습니다.");
        }
    }


    private void upsertCart(AddCartItemRequestDto reqDto, Long memberId, ProductDetail productDetail, Member member) {
        if(cartRepository.existsByMemberIdAndProductDetailId(memberId, productDetail.getId())) {

            Cart cart = cartRepository.findByProductDetailIdAndMemberId(productDetail.getId(), memberId)
                    .orElseThrow(CartException::badRequestCartException);

            ///  장바구니에 담긴 상품의 디테일이 같고 수량도 같다면 담긴상품
            if(cart.getQuantity() == reqDto.quantity()) {
                throw CartException.badRequestCartException("이미 장바구니에 담긴 상품입니다.");
            }
            ///  디테일이 같고 요청 수량과 저장된 수량이 다르다면 업데이트
            cart.updateQuantity(reqDto.quantity());

        } else {

            Cart cart = Cart.createDefaultCart(member, productDetail, reqDto.quantity());
            cartRepository.save(cart);
        }
    }

    public List<FindCartInfoResponseDto> findCartItemByMember() {
        Long memberId = JwtHelper.getMemberId();

        List<FindCartInfoResponseDto> respDto = new ArrayList<>();

        /// 장바구니 상품아래에 있는 각각의 상품 정보들 가져오는 직
        /// 사용자의 장바구니 아이템을 조회
        List<CartItemInfoDto> cartItemInfoByMember = cartRepository.findCartItemInfoByMemberId(memberId);

        /// 장바구니기본정보 - 디테일아이디
        Map<CartItemInfoDto, Long> cartItemProductDetailIdMapping = new HashMap<>();
        List<Long> productDetailIds = new ArrayList<>();
        for (CartItemInfoDto dto : cartItemInfoByMember) {
            cartItemProductDetailIdMapping.put(dto, dto.productDetailId());
            productDetailIds.add(dto.productDetailId());
        }

        List<ProductInfoForCartDto> productInfoByDetailIds = productDetailService.findProductInfoByDetailIds(productDetailIds);
        productDetailService.findProductDetailOptionForCart(productDetailIds);
        /// 디테일 아이디 - 상품기본정보
        Map<Long, ProductInfoForCartDto> productDetailIdProductInfoMapping = new HashMap<>();
        for (ProductInfoForCartDto dto : productInfoByDetailIds) {
            productDetailIdProductInfoMapping.put(dto.detailId(), dto);
        }

        /// 디테일 아이디 - 담은 상품의정보
        cartItemProductDetailIdMapping.entrySet().forEach(entry -> {

            CartItemInfoDto CartItemInfoDto = entry.getKey();
            ProductInfoForCartDto productInfoForCartDto = productDetailIdProductInfoMapping.get(entry.getValue());

        });

        /// 상품의 옵션정보. 상품아이디와 매핑되어야함.



    }
}
