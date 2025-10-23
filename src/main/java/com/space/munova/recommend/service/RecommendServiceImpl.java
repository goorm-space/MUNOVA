package com.space.munova.recommend.service;

import com.space.munova.member.entity.Member;
import com.space.munova.product.domain.Product;
import com.space.munova.recommend.domain.ProductRecommendation;
import com.space.munova.recommend.domain.UserRecommendation;
import com.space.munova.recommend.dto.ResponseDto;
import com.space.munova.recommend.repository.ProductRecommendationRepository;
import com.space.munova.recommend.repository.ProductRepository;
import com.space.munova.recommend.repository.UserRecommendationRepository;
import com.space.munova.recommend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final UserRecommendationRepository userRecommendRepository;
    private final ProductRecommendationRepository productRecommendRepository;

    @Override
    public List<ResponseDto> getRecommendationsByUserId(Long userId) {
        //회원 추천 로그 조회 로직
        return Collections.emptyList();
    }

    @Override
    public void createUserRecommendLog(Long userId) {
        //회원 추천 로그 로직
        System.out.println("User recommend log created for userId = " + userId);
    }

    @Override
    public void updateUserRecommendLog(Long userId) {
        //회원 추천 로그 로직
        System.out.println("User recommend log updated for userId = " + userId);
    }

    @Override
    public List<ResponseDto> getRecommendationsByProductId(Long productId) {
        //상품 추천 로그 조회 로직
        return Collections.emptyList();
    }

    @Override
    public void createProductRecommendLog() {
        //상품 추천 로그 로직
        System.out.println("Product recommend log created");
    }

    @Override
    public void updateProductRecommendLog(Long productId) {
        //상품 추천 로그 로직
        System.out.println("Product recommend log updated for productId = " + productId);
    }

    @Override
    @Transactional
    public void updateUserProductRecommend(Long userId, Long productId) {
        // 1. 사용자 조회
        Member user = userRepository.findById(userId)
                .orElseThrow();

        // 2. 클릭/좋아요/장바구니 기준 상품 조회
        Product clickedProduct = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // 3. 같은 카테고리에서 4개 추천 상품 조회
        List<Product> recommendedProducts = productRepository
                .findTop4ByCategory_IdAndIdNotOrderByIdAsc(clickedProduct.getCategory().getId(), clickedProduct.getId());

        // 4. 추천 기록 저장
        for (Product rec : recommendedProducts) {
            UserRecommendation ur = UserRecommendation.builder()
                    .member(user)
                    .product(rec)
                    .build();
            userRecommendRepository.save(ur);
        }

        System.out.println("User recommendations updated for user " + userId + " based on product " + productId);
    }

    @Override
    @Transactional
    public void updateSimilarProductRecommend(Long productId) {
        // 1. 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        productRecommendRepository.deleteBySourceProduct(product);

        if (product.getCategory() == null) {
            System.out.println("Product id=" + productId + " has no category. Cannot generate recommendations.");
            return; // 추천 로직 종료
        }

        // 2. 같은 카테고리에서 4개 유사 상품 조회 (본 상품 제외)
        Long categoryId = product.getCategory().getId();
        List<Product> Recommendations = productRepository
                .findTop4ByCategory_IdAndIdNotOrderByIdAsc(categoryId, product.getId());

        if (Recommendations == null || Recommendations.isEmpty()) {
            System.out.println("추천할 상품이 없습니다.");
            return;
        }
        // 3. 추천 기록 저장
        for (Product rec : Recommendations) {
            ProductRecommendation pr = ProductRecommendation.builder()
                    .sourceProduct(product)
                    .targetProduct(rec)
                    .build();
            productRecommendRepository.save(pr);

            System.out.println(product.getName() + " -> " + rec.getName());
        }

        System.out.println("Similar products updated for productId = " + productId);
    }

    @Override
    public List<ResponseDto> getRecommendationReason(Long userId, Long productId) {
        //추천 근거 조회
        return Collections.emptyList();
    }

    @Override
    public double getRecommendationScore(Long userId, Long productId) {
        //추천 점수 조회
        return 0.0;
    }
}