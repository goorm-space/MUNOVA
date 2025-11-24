package com.space.munova.order.entity;

import com.space.munova.order.dto.OrderStatus;
import com.space.munova.product.domain.ProductDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrderItemTest {

    private Order mockOrder;
    private ProductDetail mockProductDetail;
    private final String PRODUCT_NAME = "테스트 상품 이름";
    private final long PRODUCT_PRICE = 15000L;
    private final int QUANTITY = 3;

    @BeforeEach
    void setUp() {
        // 협력 객체 Mocking
        mockOrder = mock(Order.class);
        mockProductDetail = mock(ProductDetail.class);

        // ProductDetail Mock 설정 (리팩토링된 create 메서드 지원)
        when(mockProductDetail.getNameSnapshot()).thenReturn(PRODUCT_NAME);
        when(mockProductDetail.getPriceSnapshot()).thenReturn(PRODUCT_PRICE);
    }

    // --- 1. create (정적 팩토리 메서드) 테스트 ---
    @Test
    @DisplayName("create: OrderItem 객체를 올바른 스냅샷 값으로 초기화해야 한다")
    void create_ShouldInitializeWithCorrectSnapshots() {
        // WHEN
        OrderItem orderItem = OrderItem.create(mockOrder, mockProductDetail, QUANTITY);

        // THEN
        // 1. 객체 기본 상태 검증
        assertThat(orderItem).isNotNull();
        assertThat(orderItem.getOrder()).isEqualTo(mockOrder);
        assertThat(orderItem.getProductDetail()).isEqualTo(mockProductDetail);

        // 2. 스냅샷 데이터 검증
        assertThat(orderItem.getNameSnapshot()).isEqualTo(PRODUCT_NAME);
        assertThat(orderItem.getPriceSnapshot()).isEqualTo(PRODUCT_PRICE);
        assertThat(orderItem.getQuantity()).isEqualTo(QUANTITY);
        assertThat(orderItem.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    // --- 2. calculateAmount 테스트 ---
    @Test
    @DisplayName("calculateAmount: 가격 스냅샷과 수량의 곱을 정확히 반환해야 한다")
    void calculateAmount_ShouldReturnPriceTimesQuantity() {
        // GIVEN
        long expectedAmount = PRODUCT_PRICE * QUANTITY;

        // OrderItem 객체 직접 생성 및 필드 설정 (테스트를 위해 빌더 사용 가정)
        OrderItem orderItem = OrderItem.builder()
                .priceSnapshot(PRODUCT_PRICE)
                .quantity(QUANTITY)
                .build();

        // WHEN
        long actualAmount = orderItem.calculateAmount();

        // THEN
        assertThat(actualAmount).isEqualTo(expectedAmount);
    }

    @Test
    @DisplayName("calculateAmount: 수량이 0일 때 총액은 0을 반환해야 한다")
    void calculateAmount_WhenQuantityIsZero_ShouldReturnZero() {
        // GIVEN
        OrderItem orderItem = OrderItem.builder()
                .priceSnapshot(PRODUCT_PRICE)
                .quantity(0)
                .build();

        // WHEN
        long actualAmount = orderItem.calculateAmount();

        // THEN
        assertThat(actualAmount).isZero();
    }

    // --- 3. updateStatus 테스트 ---

    @Test
    @DisplayName("updateStatus: OrderItem의 상태를 주어진 상태로 변경해야 한다")
    void updateStatus_ShouldUpdateTheStatus() {
        // GIVEN
        OrderItem orderItem = OrderItem.create(mockOrder, mockProductDetail, 1);
        final OrderStatus NEW_STATUS = OrderStatus.SHIPPING;

        // 초기 상태 확인
        assertThat(orderItem.getStatus()).isEqualTo(OrderStatus.CREATED);

        // WHEN
        orderItem.updateStatus(NEW_STATUS);

        // THEN
        assertThat(orderItem.getStatus()).isEqualTo(NEW_STATUS);
    }
}
