package com.space.munova.product.domain;

import com.space.munova.member.entity.Member;
import com.space.munova.product.application.exception.ProductDetailException;
import com.space.munova.product.domain.enums.ProductCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProductDetail 도메인 단위 테스트")
class ProductDetailTest {

    private Product createProduct() {
        Brand brand = new Brand(1L, "대표자명", "BRAND001", "테스트 브랜드");
        Category category = new Category(1L, null, ProductCategory.M_SNEAKERS, "테스트 카테고리", 1);
        Member member = Member.createMember("testuser", "encodedPassword", "서울시");
        return Product.createDefaultProduct(
                "테스트 상품",
                "이것은 테스트 상품입니다. 최소 10자 이상입니다.",
                100000L,
                brand,
                category,
                member
        );
    }

    @Test
    @DisplayName("상품 상세 생성 성공")
    void createDefaultProductDetail_Success() {
        // Given
        Product product = createProduct();
        Integer quantity = 10;

        // When
        ProductDetail productDetail = ProductDetail.createDefaultProductDetail(product, quantity);

        // Then: 비즈니스 로직 검증
        assertNotNull(productDetail);
        assertEquals(product, productDetail.getProduct());
        assertEquals(quantity, productDetail.getQuantity());
    }

    @Test
    @DisplayName("상품 상세 생성 실패 - 수량 null")
    void createDefaultProductDetail_QuantityNull() {
        // Given
        Product product = createProduct();
        Integer quantity = null;

        // When & Then: 비즈니스 로직 검증 - 예외 발생
        assertThrows(IllegalArgumentException.class, () -> {
            ProductDetail.createDefaultProductDetail(product, quantity);
        });
    }

    @Test
    @DisplayName("상품 상세 생성 실패 - 수량 1 미만")
    void createDefaultProductDetail_QuantityLessThanOne() {
        // Given
        Product product = createProduct();
        Integer quantity = 0;  // 1 미만

        // When & Then: 비즈니스 로직 검증 - 예외 발생
        assertThrows(IllegalArgumentException.class, () -> {
            ProductDetail.createDefaultProductDetail(product, quantity);
        });
    }

    @Test
    @DisplayName("재고 차감 성공")
    void deductStock_Success() {
        // Given
        Product product = createProduct();
        ProductDetail productDetail = ProductDetail.builder()
                .product(product)
                .quantity(10)  // 초기 재고 10개
                .build();

        // When
        productDetail.deductStock(5);

        // Then: 비즈니스 로직 검증
        assertEquals(5, productDetail.getQuantity());
    }

    @Test
    @DisplayName("재고 차감 실패 - 재고 부족")
    void deductStock_InsufficientStock() {
        // Given
        Product product = createProduct();
        ProductDetail productDetail = ProductDetail.builder()
                .product(product)
                .quantity(5)  // 재고 5개
                .build();

        // When & Then: 비즈니스 로직 검증 - 예외 발생
        assertThrows(ProductDetailException.class, () -> {
            productDetail.deductStock(10);  // 10개 요청 (재고 부족)
        });
    }

    @Test
    @DisplayName("재고 복구 성공")
    void increaseStock_Success() {
        // Given
        Product product = createProduct();
        ProductDetail productDetail = ProductDetail.builder()
                .product(product)
                .quantity(10)  // 초기 재고 10개
                .build();

        // When
        productDetail.increaseStock(5);

        // Then: 비즈니스 로직 검증
        assertEquals(15, productDetail.getQuantity());
    }

    @Test
    @DisplayName("재고 복구 성공 - 재고가 null인 경우")
    void increaseStock_WhenQuantityIsNull() {
        // Given
        Product product = createProduct();
        ProductDetail productDetail = ProductDetail.builder()
                .product(product)
                .quantity(null)  // null인 경우
                .build();

        // When
        productDetail.increaseStock(5);

        // Then: 비즈니스 로직 검증 - null이면 0으로 초기화 후 추가
        assertEquals(5, productDetail.getQuantity());
    }
}

