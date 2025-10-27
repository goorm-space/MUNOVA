package com.space.munova.recommend.service;

import com.space.munova.product.domain.Brand;
import com.space.munova.product.domain.Category;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.Repository.ProductRepository;
import com.space.munova.product.domain.enums.ProductCategory;
import com.space.munova.recommend.domain.UserRecommendation;
import com.space.munova.recommend.dto.RecommendReasonResponseDto;
import com.space.munova.recommend.repository.UserRecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

//Product에 @Setter 추가 후 테스트
@ExtendWith(MockitoExtension.class)
class RecommendReasonTest {

    @Mock
    private UserRecommendationRepository userRecommendRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private RecommendServiceImpl recommendService;

    private Product baseProduct;
    private Product targetProduct;

    @BeforeEach
    void setUp() {
        Brand nike = new Brand(1L, "대표자이름", "NIKE01", "NIKE");
        Category sneakersCategory = new Category(1L,null,ProductCategory.M_SNEAKERS,2);
        Category bootsCategory = new Category(2L,null,ProductCategory.M_BOOTS,2);
        this.baseProduct=Product.builder()
                .id(1L)
                .name("나이키 스니커즈")
                .brand(nike)
                .price(99000L)
                .category(sneakersCategory)
                .build();
        this.targetProduct=Product.builder()
                .id(2L)
                .name("아디다스 스니커즈")
                .brand(nike)
                .price(95000L)
                .category(bootsCategory)
                .build();
    }

    @Test
    void testGetRecommendationReason() {
        // given
        Long userId = 100L;
        Long productId = targetProduct.getId();

        UserRecommendation recentLog = new UserRecommendation();
        recentLog.setProduct(baseProduct);

        when(userRecommendRepository.findTopByMemberIdOrderByCreatedAtDesc(userId))
                .thenReturn(Optional.of(recentLog));

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(targetProduct));

        // when
        List<RecommendReasonResponseDto> reasons = recommendService.getRecommendationReason(userId, productId);

        // then
        for (RecommendReasonResponseDto r : reasons) {
            switch (r.getType()) {
                case "category" -> assertThat(r.getReason()).contains("카테고리");
                case "brand" -> assertThat(r.getReason()).contains("브랜드");
                case "price" -> assertThat(r.getReason()).contains("가격");
                case "name" -> assertThat(r.getReason()).contains("이름");
            }
        }

        System.out.println("✅ 추천 이유 테스트 결과:");
        reasons.forEach(r -> System.out.println(" - " + r.getType() + ": " + r.getReason()));
    }
}