package com.space.munova.order.service;

import com.space.munova.order.entity.Order;
import com.space.munova.order.exception.OrderException;
import com.space.munova.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class OrderQueryServiceImplTest {

    @InjectMocks
    private OrderQueryServiceImpl orderQueryService;

    @Mock
    private OrderRepository orderRepository;

    @Test
    @DisplayName("[주문조회] (HappyCase) 유효한 주문 번호로 주문을 조회한다.")
    void getOrderByOrderNum_HappyCase() {
        // GIVEN
        Order mockOrder = mock(Order.class);
        String VALID_ORDER_NUM = "ORD123456";
        when(orderRepository.findByOrderNum(VALID_ORDER_NUM))
                .thenReturn(Optional.of(mockOrder));

        // WHEN
        Order result = orderQueryService.getOrderByOrderNum(VALID_ORDER_NUM);

        // THEN
        verify(orderRepository, times(1)).findByOrderNum(VALID_ORDER_NUM);
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mockOrder);
    }

    @Test
    @DisplayName("[주문조회] 주문 번호가 존재하지 않으면 OrderException을 발생시킨다.")
    void getOrderByOrderNum_NotFound_ThrowsException() {
        // GIVEN
        String INVALID_ORDER_NUM = "ORD999999";
        when(orderRepository.findByOrderNum(INVALID_ORDER_NUM))
                .thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> orderQueryService.getOrderByOrderNum(INVALID_ORDER_NUM))
                .isInstanceOf(OrderException.class);

        verify(orderRepository, times(1)).findByOrderNum(INVALID_ORDER_NUM);
    }
}
