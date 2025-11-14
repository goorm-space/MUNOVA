package com.space.munova.product.application;

import com.space.munova.product.application.exception.ProductDetailException;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.product.domain.Repository.ProductDetailRepository;
import org.hibernate.query.sqm.mutation.internal.cte.CteInsertStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductDetailService 단위 테스트")
class ProductDetailServiceTest {

    @Mock
    private ProductDetailRepository productDetailRepository;

    @InjectMocks
    private ProductDetailService productDetailService;

    // 공통으로 사용할 변수들
    private Long productDetailId;
    private ProductDetail baseProductDetail;

    @BeforeEach
    void setUp() {
        // 각 테스트 실행 전에 공통 설정
        productDetailId = 1L;
        
        // 기본 ProductDetail 객체 생성 (각 테스트에서 필요에 따라 수정해서 사용)
        baseProductDetail = ProductDetail.builder()
                .id(productDetailId)
                .quantity(10)  // 기본 재고 10개
                .build();
    }

    @Test
    @DisplayName("재고 차감 성공 - 비즈니스 로직 검증")
    void deductStock_Success() {
        // Given
        int requestQuantity = 5;

        // @BeforeEach에서 만든 기본 객체 사용 (또는 복사해서 사용)
        ProductDetail realProductDetail = ProductDetail.builder()
                .id(baseProductDetail.getId())
                .quantity(baseProductDetail.getQuantity())  // 기본 재고 10개 사용
                .build();

        // Mock Repository가 실제 객체를 반환하도록 설정
        when(productDetailRepository.findByIdWithPessimisticLock(productDetailId))
                .thenReturn(Optional.of(realProductDetail));

        // When
        ProductDetail result = productDetailService.deductStock(productDetailId, requestQuantity);

        // Then: 비즈니스 로직 검증 - 재고가 차감되었는지 확인
        assertEquals(5, result.getQuantity());  // 10 - 5 = 5
        assertEquals(5, realProductDetail.getQuantity());  // 실제 객체도 변경됨
        verify(productDetailRepository).findByIdWithPessimisticLock(productDetailId);
    }

    @Test
    @DisplayName("재고 차감 실패 - 재고 없음")
    void deductStock_NoStock() {
        // Given
        int requestQuantity = 5;

        // @BeforeEach의 기본 객체를 기반으로 재고 0개로 수정
        ProductDetail realProductDetail = ProductDetail.builder()
                .id(baseProductDetail.getId())
                .quantity(0)  // 재고 없음
                .build();

        when(productDetailRepository.findByIdWithPessimisticLock(productDetailId))
                .thenReturn(Optional.of(realProductDetail));

        // When & Then: 비즈니스 로직 검증 - 재고가 0이면 예외 발생
        assertThrows(ProductDetailException.class, () -> {
            productDetailService.deductStock(productDetailId, requestQuantity);
        });

        // 재고는 변경되지 않아야 함
        assertEquals(0, realProductDetail.getQuantity());
        verify(productDetailRepository).findByIdWithPessimisticLock(productDetailId);
    }

    @Test
    @DisplayName("재고 차감 실패 - 재고 부족")
    void deductStock_InsufficientStock() {
        // Given
        int requestQuantity = 10;
        int initialStock = 3;  // 재고 부족

        // @BeforeEach의 기본 객체를 기반으로 재고 3개로 수정
        ProductDetail realProductDetail = ProductDetail.builder()
                .id(baseProductDetail.getId())
                .quantity(initialStock)  // 재고 3개만 있음
                .build();

        when(productDetailRepository.findByIdWithPessimisticLock(productDetailId))
                .thenReturn(Optional.of(realProductDetail));

        // When & Then: 비즈니스 로직 검증 - 재고가 요청량보다 적으면 예외 발생
        assertThrows(ProductDetailException.class, () -> {
            productDetailService.deductStock(productDetailId, requestQuantity);
        });

        // 재고는 변경되지 않아야 함
        assertEquals(3, realProductDetail.getQuantity());
        verify(productDetailRepository).findByIdWithPessimisticLock(productDetailId);
    }


}

