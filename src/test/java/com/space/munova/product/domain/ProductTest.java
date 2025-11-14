package com.space.munova.product.domain;

import com.space.munova.member.entity.Member;
import com.space.munova.product.application.exception.ProductException;
import com.space.munova.product.domain.enums.ProductCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product 도메인 단위 테스트")
class ProductTest {

    // Mock 없이 실제 객체만 사용
    private Brand createBrand() {
        return new Brand(1L, "대표자명", "BRAND001", "테스트 브랜드");
    }

    private Category createCategory() {
        return new Category(1L, null, ProductCategory.M_SNEAKERS, "테스트 카테고리", 1);
    }

    private Member createMember() {
        return Member.createMember("testuser", "encodedPassword", "서울시");
    }

    @Test
    @DisplayName("상품 생성 성공")
    void createDefaultProduct_Success() {
        // Given
        String name = "테스트 상품";
        String info = "이것은 테스트 상품입니다. 최소 10자 이상입니다.";
        Long price = 100000L;
        Brand brand = createBrand();
        Category category = createCategory();
        Member member = createMember();

        // When
        Product product = Product.createDefaultProduct(name, info, price, brand, category, member);

        // Then: 비즈니스 로직 검증
        assertNotNull(product);
        assertEquals(name, product.getName());
        assertEquals(info, product.getInfo());
        assertEquals(price, product.getPrice());
        assertEquals(brand, product.getBrand());
        assertEquals(category, product.getCategory());
        assertEquals(member, product.getMember());
    }

    @Test
    @DisplayName("상품 생성 실패 - 브랜드 null")
    void createDefaultProduct_BrandNull() {
        // Given
        String name = "테스트 상품";
        String info = "이것은 테스트 상품입니다. 최소 10자 이상입니다.";
        Long price = 100000L;
        Brand brand = null;  // null
        Category category = createCategory();
        Member member = createMember();

        // When & Then: 비즈니스 로직 검증 - 예외 발생
        assertThrows(IllegalArgumentException.class, () -> {
            Product.createDefaultProduct(name, info, price, brand, category, member);
        });
    }

    @Test
    @DisplayName("상품 생성 실패 - 가격 음수")
    void createDefaultProduct_NegativePrice() {
        // Given
        String name = "테스트 상품";
        String info = "이것은 테스트 상품입니다. 최소 10자 이상입니다.";
        Long price = -1000L;  // 음수
        Brand brand = createBrand();
        Category category = createCategory();
        Member member = createMember();

        // When & Then: 비즈니스 로직 검증 - 예외 발생
        assertThrows(IllegalArgumentException.class, () -> {
            Product.createDefaultProduct(name, info, price, brand, category, member);
        });
    }

    @Test
    @DisplayName("상품 생성 실패 - 상품 정보 10자 미만")
    void createDefaultProduct_InfoTooShort() {
        // Given
        String name = "테스트 상품";
        String info = "짧음";  // 10자 미만
        Long price = 100000L;
        Brand brand = createBrand();
        Category category = createCategory();
        Member member = createMember();

        // When & Then: 비즈니스 로직 검증 - 예외 발생
        assertThrows(IllegalArgumentException.class, () -> {
            Product.createDefaultProduct(name, info, price, brand, category, member);
        });
    }

    @Test
    @DisplayName("상품 수정 성공")
    void updateProduct_Success() {
        // Given
        Product product = Product.builder()
                .name("기존 상품명")
                .info("기존 상품 정보입니다. 최소 10자 이상입니다.")
                .price(50000L)
                .build();

        String newName = "수정된 상품명";
        String newInfo = "수정된 상품 정보입니다. 최소 10자 이상입니다.";
        Long newPrice = 150000L;

        // When
        product.updateProduct(newName, newInfo, newPrice);

        // Then: 비즈니스 로직 검증
        assertEquals(newName, product.getName());
        assertEquals(newInfo, product.getInfo());
        assertEquals(newPrice, product.getPrice());
    }

    @Test
    @DisplayName("상품 수정 실패 - 음수 가격")
    void updateProduct_NegativePrice() {
        // Given
        Product product = Product.builder()
                .name("기존 상품명")
                .info("기존 상품 정보입니다. 최소 10자 이상입니다.")
                .price(50000L)
                .build();

        Long negativePrice = -1000L;

        // When & Then: 비즈니스 로직 검증 - 예외 발생
        assertThrows(ProductException.class, () -> {
            product.updateProduct("수정명", "수정된 상품 정보입니다.", negativePrice);
        });
    }

    @Test
    @DisplayName("상품 수정 실패 - 상품명 null")
    void updateProduct_NameNull() {
        // Given
        Product product = Product.builder()
                .name("기존 상품명")
                .info("기존 상품 정보입니다. 최소 10자 이상입니다.")
                .price(50000L)
                .build();

        String nullName = null;
        String validInfo = "수정된 상품 정보입니다. 최소 10자 이상입니다.";
        Long validPrice = 150000L;

        // When & Then: 비즈니스 로직 검증 - 예외 발생
        assertThrows(ProductException.class, () -> {
            product.updateProduct(nullName, validInfo, validPrice);
        });
    }

    @Test
    @DisplayName("좋아요 증가 성공")
    void plusLike_Success() {
        // Given
        Product product = Product.builder()
                .likeCount(5)
                .build();

        // When
        product.plusLike();

        // Then: 비즈니스 로직 검증
        assertEquals(6, product.getLikeCount());
    }

    @Test
    @DisplayName("좋아요 감소 성공")
    void minusLike_Success() {
        // Given
        Product product = Product.builder()
                .likeCount(5)
                .build();

        // When
        product.minusLike();

        // Then: 비즈니스 로직 검증
        assertEquals(4, product.getLikeCount());
    }

    @Test
    @DisplayName("좋아요 감소 실패 - 좋아요 수 0 이하")
    void minusLike_ZeroLikeCount() {
        // Given
        Product product = Product.builder()
                .likeCount(0)  // 좋아요 0개
                .build();

        // When & Then: 비즈니스 로직 검증 - 예외 발생
        assertThrows(ProductException.class, () -> {
            product.minusLike();
        });
    }

    @Test
    @DisplayName("좋아요 감소 실패 - 좋아요 수가 이미 음수인 경우")
    void minusLike_NegativeLikeCount() {
        // Given
        Product product = Product.builder()
                .likeCount(-1)  // 이미 음수인 상태
                .build();

        // When & Then: 비즈니스 로직 검증 - 예외 발생
        assertThrows(ProductException.class, () -> {
            product.minusLike();
        });
    }

    @Test
    @DisplayName("판매량 증가 성공")
    void plusSalesCount_Success() {
        // Given
        Product product = Product.builder()
                .salesCount(10)
                .build();

        // When
        product.plusSalesCount(5);

        // Then: 비즈니스 로직 검증
        assertEquals(15, product.getSalesCount());
    }

    @Test
    @DisplayName("판매량 감소 성공")
    void minusSalesCount_Success() {
        // Given
        Product product = Product.builder()
                .salesCount(10)
                .build();

        // When
        product.minusSalesCount(3);

        // Then: 비즈니스 로직 검증
        assertEquals(7, product.getSalesCount());
    }

    @Test
    @DisplayName("판매량 감소 실패 - 판매량 음수")
    void minusSalesCount_NegativeResult() {
        // Given
        Product product = Product.builder()
                .salesCount(5)
                .build();

        // When & Then: 비즈니스 로직 검증 - 예외 발생
        assertThrows(ProductException.class, () -> {
            product.minusSalesCount(10);  // 5 - 10 = -5 (음수)
        });
    }

    @Test
    @DisplayName("판매량 감소 실패 - 차감할 판매량 음수")
    void minusSalesCount_NegativeParameter() {
        // Given
        Product product = Product.builder()
                .salesCount(10)
                .build();

        int negativeSalesCount = -5;  // 음수 파라미터

        // When & Then: 비즈니스 로직 검증 - 예외 발생
        assertThrows(ProductException.class, () -> {
            product.minusSalesCount(negativeSalesCount);
        });
    }

    @Test
    @DisplayName("상품 삭제 성공")
    void deleteProduct_Success() {
        // Given
        Product product = Product.builder()
                .isDeleted(false)
                .build();

        // When
        product.deleteProduct();

        // Then: 비즈니스 로직 검증
        assertTrue(product.isDeleted());
    }
}

