package com.space.munova.order.entity;

import com.space.munova.member.entity.Member;
import com.space.munova.order.dto.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class OrderTest {

    private Member mockMember;
    private Order order;
    private OrderItem mockOrderItem1;
    private OrderItem mockOrderItem2;

    @BeforeEach
    void setUp() {
        mockMember = mock(Member.class);
        order = Order.createOrder(mockMember, null);

        mockOrderItem1 = mock(OrderItem.class);
        mockOrderItem2 = mock(OrderItem.class);

        order.addOrderItem(mockOrderItem1);
        order.addOrderItem(mockOrderItem2);
    }

    @DisplayName("createOrder 팩토리 메서드는 초기 주문 상태로 Order 객체를 생성해야 한다")
    @Test
    void createOrder_ShouldCreateInitialOrder() {
        // given
        String userRequest = "문 앞에 배송해주세요";
        final OrderStatus EXPECTED_STATUS = OrderStatus.CREATED;

        // when
        Order initOrder = Order.createOrder(mockMember, userRequest);

        // then
        assertThat(initOrder).isNotNull();
        assertThat(initOrder.getMember()).isSameAs(mockMember);
        assertThat(initOrder.getOrderNum()).isNotBlank();
        assertThat(initOrder.getOrderNum()).matches("^[0-9]{8}[A-Z0-9]{8}$");
        assertThat(initOrder.getUserRequest()).isEqualTo(userRequest);
        assertThat(initOrder.getStatus()).isEqualTo(EXPECTED_STATUS);
        assertThat(initOrder.getOrderItems()).isEmpty();
        assertThat(initOrder.getCouponId()).isNull();
        assertThat(initOrder.getOriginPrice()).isNull();
        assertThat(initOrder.getDiscountPrice()).isNull();
        assertThat(initOrder.getTotalPrice()).isNull();
    }

    @DisplayName("createOrder 팩토리 메서드는 배송 요청사항(userRequest)가 null이어도 초기 주문 상태로 Order 객체를 생성해야 한다")
    @Test
    void createOrder_nullUserRequest() {
        // given
        final OrderStatus EXPECTED_STATUS = OrderStatus.CREATED;

        // when
        Order initOrder = Order.createOrder(mockMember, null);

        // then
        assertThat(initOrder).isNotNull();
        assertThat(initOrder.getMember()).isSameAs(mockMember);
        assertThat(initOrder.getUserRequest()).isNull();
        assertThat(initOrder.getStatus()).isEqualTo(EXPECTED_STATUS);
    }

    @Test
    @DisplayName("addOrderItem: 새로운 OrderItem을 리스트에 정상적으로 추가해야 한다")
    void addOrderItem_ShouldAddOrderItemToCollection() {
        // GIVEN
        OrderItem newOrderItem = mock(OrderItem.class);
        int initialSize = order.getOrderItems().size(); // 2개

        // WHEN
        order.addOrderItem(newOrderItem);

        // THEN
        assertThat(order.getOrderItems()).hasSize(initialSize + 1);
        assertThat(order.getOrderItems()).contains(newOrderItem);
    }

    // --- 2. updateOrder (최종 금액/상태 업데이트) 테스트 ---
    @Test
    @DisplayName("updateOrder: 금액 정보와 상태를 업데이트하고 OrderItem의 상태를 PAYMENT_PENDING으로 전파해야 한다")
    void updateOrder_ShouldUpdateAllFieldsAndPropagateStatus() {
        // GIVEN
        final Long ORIGIN_PRICE = 50000L;
        final Long DISCOUNT_PRICE = 5000L;
        final Long TOTAL_PRICE = 45000L;
        final Long COUPON_ID = 99L;
        final OrderStatus EXPECTED_STATUS = OrderStatus.PAYMENT_PENDING;

        // WHEN
        order.updateOrder(ORIGIN_PRICE, DISCOUNT_PRICE, TOTAL_PRICE, COUPON_ID);

        // THEN
        // 1. Order 엔티티 상태 검증
        assertThat(order.getOriginPrice()).isEqualTo(ORIGIN_PRICE);
        assertThat(order.getDiscountPrice()).isEqualTo(DISCOUNT_PRICE);
        assertThat(order.getTotalPrice()).isEqualTo(TOTAL_PRICE);
        assertThat(order.getCouponId()).isEqualTo(COUPON_ID);
        assertThat(order.getStatus()).isEqualTo(EXPECTED_STATUS);

        // 2. OrderItem 상태 전파 검증
        // 두 개의 Mock OrderItem 객체 모두 updateStatus(PAYMENT_PENDING)이 호출되었는지 확인
        verify(mockOrderItem1, times(1)).updateStatus(EXPECTED_STATUS);
        verify(mockOrderItem2, times(1)).updateStatus(EXPECTED_STATUS);
    }

    // --- 3. updateStatus (일반 상태 업데이트) 테스트 ---
    @Test
    @DisplayName("updateStatus: Order의 상태를 변경하고 OrderItem의 상태를 변경된 상태로 전파해야 한다")
    void updateStatus_ShouldChangeStatusAndPropagateToOrderItems() {
        // GIVEN
        final OrderStatus NEW_STATUS = OrderStatus.DELIVERED;

        // 초기 상태 확인 (Optional)
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);

        // WHEN
        order.updateStatus(NEW_STATUS);

        // THEN
        // 1. Order 엔티티 상태 검증
        assertThat(order.getStatus()).isEqualTo(NEW_STATUS);

        // 2. OrderItem 상태 전파 검증
        // 두 개의 Mock OrderItem 객체 모두 updateStatus(DELIVERING)이 호출되었는지 확인
        verify(mockOrderItem1, times(1)).updateStatus(NEW_STATUS);
        verify(mockOrderItem2, times(1)).updateStatus(NEW_STATUS);
    }

}
