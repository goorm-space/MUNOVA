package com.space.munova.recommend.service;

import com.space.munova.product.domain.Product;
import com.space.munova.recommend.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
public class RecommendServiceTest {

    @Autowired
    private ProductRepository productRepository;

    // 🔥 트랜잭션 롤백 방지: DB 데이터 그대로 조회 가능
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testSimilarProductRecommendation() {
        Long testProductId = 1L; // 실제 DB에 존재하는 상품 ID로 수정 가능

        // 1️⃣ 상품 조회
        Product product = productRepository.findById(testProductId)
                .orElseThrow(() -> new RuntimeException("❌ 상품을 찾을 수 없습니다. (ID: " + testProductId + ")"));
        System.out.println("❌❌❌❌❌❌"+product.getCategory());
        // 2️⃣ 동일 카테고리의 추천 상품 4개 조회 (본 상품 제외)
        List<Product> recommendations = productRepository
                .findTop4ByCategoryIdAndIdNotOrderByIdAsc(product.getCategory(), product.getId());

        // 3️⃣ 결과 출력
        System.out.println("\n✅ [추천 상품 결과]");
        System.out.println("기준 상품: " + product.getName());
        if (recommendations.isEmpty()) {
            System.out.println("추천 상품이 없습니다.");
        } else {
            recommendations.forEach(r ->
                    System.out.println(" - 추천: " + r.getName())
            );
        }
        System.out.println("========================================\n");
    }
}