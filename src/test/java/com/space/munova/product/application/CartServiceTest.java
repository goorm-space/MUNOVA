package com.space.munova.product.application;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.member.entity.Member;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.application.dto.cart.*;
import com.space.munova.product.domain.Cart;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.product.domain.Repository.CartRepository;
import com.space.munova.recommend.service.RecommendService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService 단위 테스트")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProductDetailService productDetailService;

    @Mock
    private RecommendService recommendService;

    @InjectMocks
    private CartService cartService;

    private Member createMember() {
        return Member.createMember("testuser", "encodedPassword", "서울시");
    }

    private ProductDetail createProductDetail(Long detailId, Integer quantity) {
        Product product = Product.builder()
                .id(1L)
                .name("테스트 상품")
                .build();

        return ProductDetail.builder()
                .id(detailId)
                .product(product)
                .quantity(quantity)
                .isDeleted(false)
                .build();
    }

    private Cart createCart(Long cartId, Member member, ProductDetail productDetail, int quantity) {
        return Cart.builder()
                .id(cartId)
                .member(member)
                .productDetail(productDetail)
                .quantity(quantity)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("장바구니에 새 상품 추가 성공")
    void addCartItem_NewItem_Success() {
        // Given
        Long memberId = 1L;
        Long productDetailId = 1L;
        int quantity = 2;

        Member member = createMember();
        ProductDetail productDetail = createProductDetail(productDetailId, 10);
        AddCartItemRequestDto reqDto = new AddCartItemRequestDto(productDetailId, quantity);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(productDetailService.findById(productDetailId)).thenReturn(productDetail);
        when(cartRepository.existsByMemberIdAndProductDetailId(memberId, productDetailId))
                .thenReturn(false);
        when(productDetailService.findProductIdByDetailId(productDetailId)).thenReturn(1L);
        doNothing().when(recommendService).updateUserAction(any(Long.class), anyInt(), any(), anyBoolean(), any());

        // When
        cartService.addCartItem(reqDto, memberId);

        // Then
        verify(memberRepository, times(1)).findById(memberId);
        verify(productDetailService, times(1)).findById(productDetailId);
        verify(cartRepository, times(1)).existsByMemberIdAndProductDetailId(memberId, productDetailId);
        verify(cartRepository, times(1)).save(any(Cart.class));
        verify(productDetailService, times(1)).findProductIdByDetailId(productDetailId);
        verify(recommendService, times(1)).updateUserAction(1L, 0, null, true, null);
    }

    @Test
    @DisplayName("장바구니에 기존 상품 수량 업데이트 성공")
    void addCartItem_ExistingItem_UpdateQuantity_Success() {
        // Given
        Long memberId = 1L;
        Long productDetailId = 1L;
        int newQuantity = 3;

        Member member = createMember();
        ProductDetail productDetail = createProductDetail(productDetailId, 10);
        Cart existingCart = createCart(1L, member, productDetail, 2);
        AddCartItemRequestDto reqDto = new AddCartItemRequestDto(productDetailId, newQuantity);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(productDetailService.findById(productDetailId)).thenReturn(productDetail);
        when(cartRepository.existsByMemberIdAndProductDetailId(memberId, productDetailId))
                .thenReturn(true);
        when(cartRepository.findByProductDetailIdAndMemberId(productDetailId, memberId))
                .thenReturn(Optional.of(existingCart));
        when(productDetailService.findProductIdByDetailId(productDetailId)).thenReturn(1L);
        doNothing().when(recommendService).updateUserAction(any(Long.class), anyInt(), any(), anyBoolean(), any());

        // When
        cartService.addCartItem(reqDto, memberId);

        // Then
        verify(memberRepository, times(1)).findById(memberId);
        verify(productDetailService, times(1)).findById(productDetailId);
        verify(cartRepository, times(1)).existsByMemberIdAndProductDetailId(memberId, productDetailId);
        verify(cartRepository, times(1)).findByProductDetailIdAndMemberId(productDetailId, memberId);
        verify(cartRepository, never()).save(any(Cart.class));
        assertEquals(newQuantity, existingCart.getQuantity());
    }

    @Test
    @DisplayName("장바구니 수량 수정 성공")
    void updateCartByMember_Success() {
        // Given
        Long memberId = 1L;
        Long cartId = 1L;
        Long detailId = 1L;
        Integer newQuantity = 5;

        Member member = createMember();
        ProductDetail oldProductDetail = createProductDetail(1L, 10);
        ProductDetail newProductDetail = createProductDetail(detailId, 10);
        Cart cart = createCart(cartId, member, oldProductDetail, 2);

        UpdateCartRequestDto reqDto = new UpdateCartRequestDto(cartId, detailId, newQuantity);

        when(cartRepository.findByIdAndMemberIdAndIsDeletedFalse(cartId, memberId))
                .thenReturn(Optional.of(cart));
        when(productDetailService.findById(detailId)).thenReturn(newProductDetail);

        // When
        cartService.updateCartByMemeber(reqDto, memberId);

        // Then
        verify(cartRepository, times(1)).findByIdAndMemberIdAndIsDeletedFalse(cartId, memberId);
        verify(productDetailService, times(1)).findById(detailId);
        assertEquals(newProductDetail, cart.getProductDetail());
        assertEquals(newQuantity, cart.getQuantity());
    }

    @Test
    @DisplayName("장바구니 삭제 성공")
    void deleteByCartIds_Success() {
        // Given
        Long memberId = 1L;
        List<Long> cartIds = List.of(1L, 2L);
        List<Long> productIds = List.of(1L, 2L);

        when(cartRepository.findProductIdsByCartIds(cartIds)).thenReturn(productIds);
        doNothing().when(recommendService).updateUserAction(any(Long.class), anyInt(), any(), anyBoolean(), any());
        doNothing().when(cartRepository).deleteByCartIdsAndMemberId(anyList(), any(Long.class));

        // When
        cartService.deleteByCartIds(cartIds, memberId);

        // Then
        verify(cartRepository, times(1)).findProductIdsByCartIds(cartIds);
        verify(recommendService, times(2)).updateUserAction(any(Long.class), eq(0), isNull(), eq(false), isNull());
        verify(cartRepository, times(1)).deleteByCartIdsAndMemberId(cartIds, memberId);
    }

    @Test
    @DisplayName("장바구니 조회 성공")
    void findCartItemByMember_Success() {
        // Given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        List<Long> detailIds = List.of(1L, 2L);
        Page<Long> detailIdsPage = new PageImpl<>(detailIds, pageable, 2);

        List<ProductInfoForCartDto> productInfoList = new ArrayList<>();
        // ProductInfoForCartDto 생성: productId, cartId, detailId, productName, productPrice, 
        // productQuantity, cartItemQuantity, mainImgSrc, brandName, optionId, optionType, optionName
        productInfoList.add(new ProductInfoForCartDto(
                1L, 1L, 1L, "상품1", 10000L, 10, 2,
                "image.jpg", "브랜드1", null, null, null
        ));
        productInfoList.add(new ProductInfoForCartDto(
                2L, 2L, 2L, "상품2", 20000L, 5, 1,
                "image2.jpg", "브랜드2", null, null, null
        ));

        when(cartRepository.findDistinctDetailIdsByMemberId(memberId, pageable))
                .thenReturn(detailIdsPage);
        when(cartRepository.findCartItemInfoByDetailIds(detailIds))
                .thenReturn(productInfoList);

        // When
        PagingResponse<FindCartInfoResponseDto> result = cartService.findCartItemByMember(pageable, memberId);

        // Then
        assertNotNull(result);
        verify(cartRepository, times(1)).findDistinctDetailIdsByMemberId(memberId, pageable);
        verify(cartRepository, times(1)).findCartItemInfoByDetailIds(detailIds);
    }

    @Test
    @DisplayName("장바구니 조회 - 빈 장바구니")
    void findCartItemByMember_EmptyCart() {
        // Given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Long> emptyPage = Page.empty();

        when(cartRepository.findDistinctDetailIdsByMemberId(memberId, pageable))
                .thenReturn(emptyPage);

        // When
        PagingResponse<FindCartInfoResponseDto> result = cartService.findCartItemByMember(pageable, memberId);

        // Then
        assertNotNull(result);
        verify(cartRepository, times(1)).findDistinctDetailIdsByMemberId(memberId, pageable);
        verify(cartRepository, never()).findCartItemInfoByDetailIds(anyList());
    }

    @Test
    @DisplayName("상품 상세 ID로 장바구니 삭제 성공")
    void deleteByProductDetailIds_Success() {
        // Given
        List<Long> productDetailIds = List.of(1L, 2L);

        doNothing().when(cartRepository).deleteByProductDetailIds(productDetailIds);

        // When
        cartService.deleteByProductDetailIds(productDetailIds);

        // Then
        verify(cartRepository, times(1)).deleteByProductDetailIds(productDetailIds);
    }
}
