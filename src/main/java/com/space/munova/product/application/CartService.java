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
    private final ProductImageService productImageService;

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

    public List<FindCartInfoResponseDto> findCartItemByMember(Pageable pageable) {
        Long memberId = JwtHelper.getMemberId();
        List<ProductInfoForCartDto> productInfoForCartDtos =
                cartRepository.findCartItemInfoByMemberId(memberId, pageable);

        // detailId로 그룹핑
        Map<Long, List<ProductInfoForCartDto>> groupedByDetail =
                productInfoForCartDtos.stream()
                        .collect(Collectors.groupingBy(
                                ProductInfoForCartDto::detailId,
                                LinkedHashMap::new, // 순서 보장
                                Collectors.toList()
                        ));

        // 맵들을 순회하면서 기본정보와 옵션리스트를 가진 FindCartInfoResponseDto리스트를 만들어 반환.
        return groupedByDetail.values().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /// 기존에 가져온 상품정보는 옵션을 포함한 정보들이다.
    /// 따라서 상품의 기본정보(상품아이디, 디테일아이디, 이미지등등)은 CartItemBasicInfoDto로 변환한다.
    /// 옵션은 여러가지가 올수 있다. 사이즈, 컬러 등등
    /// 따라서 리스트로 변환하여 응답데이터로 변환한다.
    private FindCartInfoResponseDto convertToResponseDto(List<ProductInfoForCartDto> productGroup) {
        ProductInfoForCartDto first = productGroup.get(0);

        // 기본 정보 생성
        CartItemBasicInfoDto basicInfo = new CartItemBasicInfoDto(
                first.productId(),
                first.cartId(),
                first.detailId(),
                first.productName(),
                first.productPrice(),
                first.productQuantity(),
                first.cartItemQuantity(),
                productImageService.getImgPath(first.mainImgSrc()),
                first.brandName()
        );

        // 옵션 정보 생성
        List<CartItemOptionInfoDto> options = productGroup.stream()
                .filter(p -> p.optionId() != null) // null이 아닌 옵션만
                .map(p -> new CartItemOptionInfoDto(
                        p.optionId(),
                        p.optionType().name(),
                        p.optionName()
                ))
                .collect(Collectors.toList());

        return new FindCartInfoResponseDto(basicInfo, options);
    }

    @Transactional(readOnly = false)
    public void updateCartByMemeber(UpdateCartRequestDto reqDto) {
        Long memberId = JwtHelper.getMemberId();
        Cart cartItem = cartRepository.findByIdAndMemberIdAndIsDeletedFalse(reqDto.cartId(), memberId).orElseThrow(CartException::badRequestCartException);
        ProductDetail productDetail = productDetailService.findById(reqDto.detailId());

        /// 더티체킹으로 카트 아이템 업데이트
        try{

            if(cartItem.isDeleted()) {
                throw new IllegalArgumentException("제거된 장바구니 상품입니다.");
            }

            if(productDetail.isDeleted()) {
                throw new IllegalArgumentException("제거된 상품입니다.");
            }

            cartItem.updateCart(productDetail, reqDto.quantity());

        }catch (Exception e){
           throw CartException.badRequestCartException(e.getMessage());
        }
    }
}
