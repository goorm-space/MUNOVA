package com.space.munova.product.domain;

import com.space.munova.member.dto.MemberRole;
import com.space.munova.member.entity.Member;
import com.space.munova.product.application.exception.ProductException;
import com.space.munova.product.domain.enums.ProductCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProductLike 도메인 단위 테스트")
class ProductLikeTest {

    private Brand createBrand() {
        return new Brand(1L, "대표자명", "BRAND001", "테스트 브랜드");
    }

    private Category createCategory() {
        return new Category(1L, null, ProductCategory.M_SNEAKERS, "테스트 카테고리", 1);
    }

    private Member createMember(Long id) {
        return Member.builder()
                .id(id)
                .username("testuser")
                .password("encodedPassword")
                .address("서울시")
                .role(MemberRole.USER)
                .build();
    }

    private Product createProduct(Long id) {
        Brand brand = createBrand();
        Category category = createCategory();
        Member seller = createMember(1L);
        return Product.builder()
                .id(id)
                .name("테스트 상품")
                .info("이것은 테스트 상품입니다. 최소 10자 이상입니다.")
                .price(100000L)
                .brand(brand)
                .category(category)
                .member(seller)
                .build();
    }

    @Test
    @DisplayName("좋아요 생성 성공")
    void createDefaultProductLike_Success() {
        // Given
        Product product = createProduct(1L);
        Member member = createMember(2L);

        // When
        ProductLike productLike = ProductLike.createDefaultProductLike(product, member);

        // Then
        assertNotNull(productLike);
        assertEquals(product, productLike.getProduct());
        assertEquals(member, productLike.getMember());
        assertFalse(productLike.isDeleted());
    }

    @Test
    @DisplayName("좋아요 생성 실패 - 상품 null")
    void createDefaultProductLike_ProductNull() {
        // Given
        Product product = null;
        Member member = createMember(2L);

        // When & Then
        assertThrows(ProductException.class, () -> {
            ProductLike.createDefaultProductLike(product, member);
        });
    }

    @Test
    @DisplayName("좋아요 생성 실패 - 멤버 null")
    void createDefaultProductLike_MemberNull() {
        // Given
        Product product = createProduct(1L);
        Member member = null;

        // When & Then
        assertThrows(ProductException.class, () -> {
            ProductLike.createDefaultProductLike(product, member);
        });
    }

    @Test
    @DisplayName("좋아요 삭제 성공 - 본인의 좋아요")
    void deleteLike_Success_OwnLike() {
        // Given
        Product product = createProduct(1L);
        Member member = createMember(2L);
        ProductLike productLike = ProductLike.createDefaultProductLike(product, member);
        Long reqMemberId = 2L; // 본인 ID

        // When
        productLike.deleteLike(reqMemberId);

        // Then
        assertTrue(productLike.isDeleted());
    }

    @Test
    @DisplayName("좋아요 삭제 실패 - 다른 멤버의 좋아요")
    void deleteLike_Fail_DifferentMember() {
        // Given
        Product product = createProduct(1L);
        Member member = createMember(2L);
        ProductLike productLike = ProductLike.createDefaultProductLike(product, member);
        Long reqMemberId = 3L; // 다른 멤버 ID

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productLike.deleteLike(reqMemberId);
        });

        assertEquals("유효하지 않은 요청입니다.", exception.getMessage());
        assertFalse(productLike.isDeleted()); // 삭제되지 않아야 함
    }

    @Test
    @DisplayName("좋아요 삭제 실패 - 요청 멤버 ID null")
    void deleteLike_Fail_RequestMemberIdNull() {
        // Given
        Product product = createProduct(1L);
        Member member = createMember(2L);
        ProductLike productLike = ProductLike.createDefaultProductLike(product, member);
        Long reqMemberId = null;

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            productLike.deleteLike(reqMemberId);
        });
        assertFalse(productLike.isDeleted()); // 삭제되지 않아야 함
    }

    @Test
    @DisplayName("좋아요 삭제 실패 - 이미 삭제된 좋아요")
    void deleteLike_Success_AlreadyDeleted() {
        // Given
        Product product = createProduct(1L);
        Member member = createMember(2L);
        ProductLike productLike = ProductLike.createDefaultProductLike(product, member);
        productLike.deleteLike(2L); // 첫 번째 삭제
        assertTrue(productLike.isDeleted());

        // When & Then - 이미 삭제된 상태에서 다시 삭제 시도
        // 현재 구현에서는 예외가 발생하지 않지만, isDeleted가 true인 상태를 유지해야 함
        productLike.deleteLike(2L);

        // Then
        assertTrue(productLike.isDeleted()); // 여전히 삭제된 상태
    }
}

