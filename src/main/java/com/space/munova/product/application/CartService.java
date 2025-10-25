package com.space.munova.product.application;

import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.application.dto.addCartItemRequestDto;
import com.space.munova.product.application.exception.CartException;
import com.space.munova.product.application.exception.ProductException;
import com.space.munova.product.domain.Cart;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.product.domain.Repository.CartRepository;
import com.space.munova.product.domain.Repository.ProductDetailRepository;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final ProductDetailRepository productDetailRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = false)
    public void deleteByProductDetailIds(List<Long> productDetailIds) {
        cartRepository.deleteByProductDetailIds(productDetailIds);
    }

    ///  카트 생성 메서드
    @Transactional(readOnly = false)
    public void addCartItem(addCartItemRequestDto reqDto) {

        Long memberId = JwtHelper.getMemberId();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberException::notFoundException);
        ProductDetail productDetail = productDetailRepository.findById((reqDto.productDetailId()))
                .orElseThrow(ProductException::notFoundProductDetailExeption);

        if(productDetail.getQuantity() < reqDto.quantity()) {
            throw CartException.notFoundProductException("상품의 수량을 초과하여 상품을 담을 수 없습니다.");
        }

        Cart cart = Cart.createDefaultCart(member, productDetail, reqDto.quantity());
        cartRepository.save(cart);
    }


    /// 유저의 장바구니 카트 상품제거
    @Transactional(readOnly = false)
    public void deleteByCartIds(List<Long> cartIds) {

        Long memberId = JwtHelper.getMemberId();
        cartRepository.deleteByCartIdsAndMemberId(cartIds,memberId);
    }
}
