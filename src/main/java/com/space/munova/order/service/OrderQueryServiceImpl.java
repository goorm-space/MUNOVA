package com.space.munova.order.service;

import com.space.munova.order.entity.Order;
import com.space.munova.order.exception.OrderException;
import com.space.munova.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryServiceImpl {

    private OrderRepository orderRepository;

    public Order getOrderByOrderNum(String orderNum) {
        return orderRepository.findByOrderNum(orderNum)
                .orElseThrow(OrderException::notFoundException);
    }
}
