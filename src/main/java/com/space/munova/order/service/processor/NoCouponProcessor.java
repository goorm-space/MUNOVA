package com.space.munova.order.service.processor;

import com.space.munova.common.validation.AmountVerifier;
import com.space.munova.order.dto.CreateOrderRequest;
import com.space.munova.order.entity.Order;
import com.space.munova.order.exception.OrderException;
import org.springframework.stereotype.Component;

@Component
public class NoCouponProcessor implements OrderAmountProcessor {

    @Override
    public void process(Order order, CreateOrderRequest request, long totalAmount) {

        try {
            AmountVerifier.verify(request.clientCalculatedAmount(), totalAmount);
        } catch (IllegalArgumentException e) {
            throw OrderException.amountMismatchException(e.getMessage());
        }

        order.updateOrder(
                totalAmount,
                0L,
                totalAmount,
                null
        );
    }
}
