package com.space.munova.order.dto;

import com.space.munova.order.exception.OrderException;
import com.space.munova.order.exception.OrderItemException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class OrderDtoTest {

    private final List<OrderItemRequest> VALID_ORDER_ITEMS = List.of(
            new OrderItemRequest(1L, 10),
            new OrderItemRequest(2L, 5)
    );
    private final Long VALID_AMOUNT = 10000L;

    @DisplayName("(HappyCase) CreateOrderRequest의 모든 필드가 유효한 경우, 객체를 생성한다.")
    @Test
    void verifyCreateOrderRequest_happyCase() {
        // given
        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                "문 앞에 놓아주세요",
                VALID_AMOUNT,
                VALID_ORDER_ITEMS
        );

        // when

        // then
        assertThat(request).isNotNull();
        assertThat(request.clientCalculatedAmount()).isEqualTo(VALID_AMOUNT);
        assertThat(request.orderItems()).hasSize(VALID_ORDER_ITEMS.size());
    }

    @DisplayName("clientCalculatedAmount가 null일 경우, OrderException이 발생해야 한다")
    @Test
    void createOrderRequest_WithNullAmount_ShouldThrowException() {
        // given
        Long nullAmount = null;

        // When & Then
        assertThatThrownBy(() -> new CreateOrderRequest(
                1L,
                "문 앞에 놓아주세요",
                nullAmount,
                VALID_ORDER_ITEMS
        )).isInstanceOf(OrderException.class);
    }

    @DisplayName("clientCalculatedAmount가 음수일 경우, OrderException이 발생해야 한다")
    @Test
    void createOrderRequest_WithNegativeAmount_ShouldThrowException() {
        // Given
        Long negativeAmount = -5000L;

        // When & Then
        assertThatThrownBy(() -> new CreateOrderRequest(
                1L,
                "문앞에 놓아주세요",
                negativeAmount,
                VALID_ORDER_ITEMS
        )).isInstanceOf(OrderException.class);
    }

    @Test
    @DisplayName("orderItems가 null일 경우, OrderItemException이 발생해야 한다")
    void createOrderRequest_WithNullOrderItems_ShouldThrowException() {
        // Given
        List<OrderItemRequest> nullItems = null;

        // When & Then
        assertThatThrownBy(() -> new CreateOrderRequest(
                1L,
                "문앞에 놓아주세요",
                VALID_AMOUNT,
                nullItems
        )).isInstanceOf(OrderItemException.class);
    }

    @Test
    @DisplayName("orderItems가 비어있는 리스트일 경우, OrderItemException이 발생해야 한다")
    void createOrderRequest_WithEmptyOrderItems_ShouldThrowException() {
        // Given
        List<OrderItemRequest> emptyItems = List.of();

        // When & Then
        assertThatThrownBy(() -> new CreateOrderRequest(
                1L,
                "문앞에 놓아주세요",
                VALID_AMOUNT,
                emptyItems
        )).isInstanceOf(OrderItemException.class);
    }
}
