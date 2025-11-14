package com.space.munova.product.domain;

import com.space.munova.member.entity.Member;
import com.space.munova.product.application.exception.CartException;
import com.space.munova.product.application.exception.ProductDetailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cart 도메인 단위 테스트")
class CartTest {

    private Member createMember() {
        return Member.createMember("testuser", "encodedPassword", "서울시");
    }

    private Product createProduct() {
        return Product.builder()
                .id(1L)
                .name("테스트 상품")
                .build();
    }

    private ProductDetail createProductDetail(Long detailId, Integer quantity, boolean isDeleted) {
        return ProductDetail.builder()
                .id(detailId)
                .product(createProduct())
                .quantity(quantity)
                .isDeleted(isDeleted)
                .build();
    }

    private Cart createCart(Long cartId, Member member, ProductDetail productDetail, int quantity, boolean isDeleted) {
        return Cart.builder()
                .id(cartId)
                .member(member)
                .productDetail(productDetail)
                .quantity(quantity)
                .isDeleted(isDeleted)
                .build();
    }

    @Test
    @DisplayName("장바구니 생성 성공")
    void createDefaultCart_Success() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);
        int quantity = 3;

        // When
        Cart cart = Cart.createDefaultCart(member, productDetail, quantity);

        // Then
        assertNotNull(cart);
        assertEquals(member, cart.getMember());
        assertEquals(productDetail, cart.getProductDetail());
        assertEquals(quantity, cart.getQuantity());
        assertFalse(cart.isDeleted());
    }

    @Test
    @DisplayName("장바구니 생성 실패 - 수량 0")
    void createDefaultCart_QuantityZero() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);
        int quantity = 0;

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            Cart.createDefaultCart(member, productDetail, quantity);
        });
    }

    @Test
    @DisplayName("장바구니 생성 실패 - 수량 음수")
    void createDefaultCart_NegativeQuantity() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);
        int quantity = -1;

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            Cart.createDefaultCart(member, productDetail, quantity);
        });
    }

    @Test
    @DisplayName("장바구니 생성 실패 - 수량 경계값 1 (성공해야 함)")
    void createDefaultCart_QuantityOne() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);
        int quantity = 1;  // 경계값

        // When
        Cart cart = Cart.createDefaultCart(member, productDetail, quantity);

        // Then
        assertNotNull(cart);
        assertEquals(1, cart.getQuantity());
    }

    @Test
    @DisplayName("장바구니 수정 실패 - null ProductDetail")
    void updateCart_NullProductDetail() {
        // Given
        Member member = createMember();
        ProductDetail oldProductDetail = createProductDetail(1L, 10, false);
        Cart cart = createCart(1L, member, oldProductDetail, 2, false);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            cart.updateCart(null, 5);
        });
    }

    @Test
    @DisplayName("장바구니 수정 실패 - 수량 경계값 (재고와 동일한 경우 성공해야 함)")
    void updateCart_QuantityEqualToStock() {
        // Given
        Member member = createMember();
        ProductDetail oldProductDetail = createProductDetail(1L, 10, false);
        ProductDetail newProductDetail = createProductDetail(2L, 10, false);  // 재고 10개
        Cart cart = createCart(1L, member, oldProductDetail, 2, false);
        int newQuantity = 10;  // 재고와 동일

        // When
        cart.updateCart(newProductDetail, newQuantity);

        // Then
        assertEquals(10, cart.getQuantity());  // 성공해야 함
    }

    @Test
    @DisplayName("장바구니 수정 실패 - 수량 경계값 (재고보다 1 많음)")
    void updateCart_QuantityOneMoreThanStock() {
        // Given
        Member member = createMember();
        ProductDetail oldProductDetail = createProductDetail(1L, 10, false);
        ProductDetail newProductDetail = createProductDetail(2L, 10, false);  // 재고 10개
        Cart cart = createCart(1L, member, oldProductDetail, 2, false);
        int newQuantity = 11;  // 재고보다 1 많음

        // When & Then
        assertThrows(ProductDetailException.class, () -> {
            cart.updateCart(newProductDetail, newQuantity);
        });
    }

    @Test
    @DisplayName("장바구니 수량 변경 실패 - 수량 경계값 (재고와 동일한 경우 성공해야 함)")
    void updateQuantity_QuantityEqualToStock() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);  // 재고 10개
        Cart cart = createCart(1L, member, productDetail, 2, false);
        int newQuantity = 10;  // 재고와 동일

        // When
        cart.updateQuantity(newQuantity);

        // Then
        assertEquals(10, cart.getQuantity());  // 성공해야 함
    }

    @Test
    @DisplayName("장바구니 수량 변경 실패 - 수량 경계값 (재고보다 1 많음)")
    void updateQuantity_QuantityOneMoreThanStock() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);  // 재고 10개
        Cart cart = createCart(1L, member, productDetail, 2, false);
        int newQuantity = 11;  // 재고보다 1 많음

        // When & Then
        assertThrows(ProductDetailException.class, () -> {
            cart.updateQuantity(newQuantity);
        });
    }

    @Test
    @DisplayName("수량 유효성 검증 - 경계값 1 (성공해야 함)")
    void validInputQuantity_BoundaryOne() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);
        Cart cart = createCart(1L, member, productDetail, 2, false);
        int validQuantity = 1;  // 경계값

        // When & Then - 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> {
            cart.validInputQuantity(validQuantity);
        });
    }

    @Test
    @DisplayName("장바구니 수정 성공")
    void updateCart_Success() {
        // Given
        Member member = createMember();
        ProductDetail oldProductDetail = createProductDetail(1L, 10, false);
        ProductDetail newProductDetail = createProductDetail(2L, 10, false);
        Cart cart = createCart(1L, member, oldProductDetail, 2, false);
        int newQuantity = 5;

        // When
        cart.updateCart(newProductDetail, newQuantity);

        // Then
        assertEquals(newProductDetail, cart.getProductDetail());
        assertEquals(newQuantity, cart.getQuantity());
    }

    @Test
    @DisplayName("장바구니 수정 실패 - 삭제된 장바구니")
    void updateCart_DeletedCart() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);
        Cart cart = createCart(1L, member, productDetail, 2, true);  // 삭제된 장바구니
        ProductDetail newProductDetail = createProductDetail(2L, 10, false);

        // When & Then
        assertThrows(CartException.class, () -> {
            cart.updateCart(newProductDetail, 5);
        });
    }

    @Test
    @DisplayName("장바구니 수정 실패 - 삭제된 상품 상세")
    void updateCart_DeletedProductDetail() {
        // Given
        Member member = createMember();
        ProductDetail oldProductDetail = createProductDetail(1L, 10, false);
        ProductDetail newProductDetail = createProductDetail(2L, 10, true);  // 삭제된 상품
        Cart cart = createCart(1L, member, oldProductDetail, 2, false);

        // When & Then
        assertThrows(ProductDetailException.class, () -> {
            cart.updateCart(newProductDetail, 5);
        });
    }

    @Test
    @DisplayName("장바구니 수정 실패 - 수량 초과")
    void updateCart_QuantityExceeded() {
        // Given
        Member member = createMember();
        ProductDetail oldProductDetail = createProductDetail(1L, 10, false);
        ProductDetail newProductDetail = createProductDetail(2L, 5, false);  // 재고 5개
        Cart cart = createCart(1L, member, oldProductDetail, 2, false);
        int newQuantity = 10;  // 재고보다 많음

        // When & Then
        assertThrows(ProductDetailException.class, () -> {
            cart.updateCart(newProductDetail, newQuantity);
        });
    }

    @Test
    @DisplayName("장바구니 수정 실패 - 수량 1 미만")
    void updateCart_QuantityLessThanOne() {
        // Given
        Member member = createMember();
        ProductDetail oldProductDetail = createProductDetail(1L, 10, false);
        ProductDetail newProductDetail = createProductDetail(2L, 10, false);
        Cart cart = createCart(1L, member, oldProductDetail, 2, false);
        int newQuantity = 0;

        // When & Then
        assertThrows(CartException.class, () -> {
            cart.updateCart(newProductDetail, newQuantity);
        });
    }

    @Test
    @DisplayName("장바구니 수량 변경 성공")
    void updateQuantity_Success() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);
        Cart cart = createCart(1L, member, productDetail, 2, false);
        int newQuantity = 5;

        // When
        cart.updateQuantity(newQuantity);

        // Then
        assertEquals(newQuantity, cart.getQuantity());
    }

    @Test
    @DisplayName("장바구니 수량 변경 실패 - 동일 수량")
    void updateQuantity_SameQuantity() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);
        Cart cart = createCart(1L, member, productDetail, 5, false);
        int sameQuantity = 5;  // 현재 수량과 동일

        // When & Then
        assertThrows(CartException.class, () -> {
            cart.updateQuantity(sameQuantity);
        });
    }

    @Test
    @DisplayName("장바구니 수량 변경 실패 - 수량 1 미만")
    void updateQuantity_QuantityLessThanOne() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);
        Cart cart = createCart(1L, member, productDetail, 2, false);
        int newQuantity = 0;

        // When & Then
        assertThrows(CartException.class, () -> {
            cart.updateQuantity(newQuantity);
        });
    }

    @Test
    @DisplayName("장바구니 수량 변경 실패 - 수량 초과")
    void updateQuantity_QuantityExceeded() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 5, false);  // 재고 5개
        Cart cart = createCart(1L, member, productDetail, 2, false);
        int newQuantity = 10;  // 재고보다 많음

        // When & Then
        assertThrows(ProductDetailException.class, () -> {
            cart.updateQuantity(newQuantity);
        });
    }

    @Test
    @DisplayName("삭제된 장바구니 체크 - 삭제된 경우 예외 발생")
    void checkDeletedCart_DeletedCart() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);
        Cart cart = createCart(1L, member, productDetail, 2, true);  // 삭제된 장바구니

        // When & Then
        assertThrows(CartException.class, () -> {
            cart.checkDeletedCart();
        });
    }

    @Test
    @DisplayName("삭제된 장바구니 체크 - 삭제되지 않은 경우 통과")
    void checkDeletedCart_NotDeleted() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);
        Cart cart = createCart(1L, member, productDetail, 2, false);  // 삭제되지 않은 장바구니

        // When & Then - 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> {
            cart.checkDeletedCart();
        });
    }

    @Test
    @DisplayName("동일 수량 체크 - 동일한 경우 예외 발생")
    void checkExistItem_SameQuantity() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);
        Cart cart = createCart(1L, member, productDetail, 5, false);
        int sameQuantity = 5;  // 현재 수량과 동일

        // When & Then
        assertThrows(CartException.class, () -> {
            cart.checkExistItem(sameQuantity);
        });
    }

    @Test
    @DisplayName("동일 수량 체크 - 다른 수량인 경우 통과")
    void checkExistItem_DifferentQuantity() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);
        Cart cart = createCart(1L, member, productDetail, 5, false);
        int differentQuantity = 3;  // 현재 수량과 다름

        // When & Then - 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> {
            cart.checkExistItem(differentQuantity);
        });
    }

    @Test
    @DisplayName("수량 유효성 검증 - 수량 1 미만 실패")
    void validInputQuantity_LessThanOne() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);
        Cart cart = createCart(1L, member, productDetail, 2, false);
        int invalidQuantity = 0;

        // When & Then
        assertThrows(CartException.class, () -> {
            cart.validInputQuantity(invalidQuantity);
        });
    }

    @Test
    @DisplayName("수량 유효성 검증 - 수량 음수 실패")
    void validInputQuantity_Negative() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);
        Cart cart = createCart(1L, member, productDetail, 2, false);
        int invalidQuantity = -1;

        // When & Then
        assertThrows(CartException.class, () -> {
            cart.validInputQuantity(invalidQuantity);
        });
    }

    @Test
    @DisplayName("수량 유효성 검증 - 유효한 수량 통과")
    void validInputQuantity_Valid() {
        // Given
        Member member = createMember();
        ProductDetail productDetail = createProductDetail(1L, 10, false);
        Cart cart = createCart(1L, member, productDetail, 2, false);
        int validQuantity = 1;

        // When & Then - 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> {
            cart.validInputQuantity(validQuantity);
        });
    }
}
