package com.space.munova.product.application;

import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.ProductLike;
import com.space.munova.product.domain.Repository.ProductLikeRepository;
import com.space.munova.product.domain.Repository.ProductRepository;
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
public class ProductLikeService {

    private final ProductLikeRepository productLikeRepository;
    private final ProductService productService;
    private final MemberRepository memberRepository;

    public void deleteProductLikeByProductId(List<Long> productIds) {
        productLikeRepository.deleteAllByProductIds(productIds);
    }

    @Transactional(readOnly = false)
    public void addLike(Long productId) {

        Long memberId = JwtHelper.getMemberId();

        Member member = memberRepository.findById(memberId).orElseThrow(MemberException::invalidMemberException);
        Product product = productService.findByIdAndIsDeletedFalse(productId);

        /// 사용자 좋아요 리스트 추가
        ProductLike productLike = ProductLike.createDefaultProductLike(product, member);
        productLikeRepository.save(productLike);

        /// 상품 좋아요수 증가.
        product.plusLike();
    }
}
