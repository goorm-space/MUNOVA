package com.space.munova.order.service;

import com.space.munova.auth.service.AuthService;
import com.space.munova.order.dto.CancelOrderItemRequest;
import com.space.munova.order.dto.CancelType;
import com.space.munova.order.dto.OrderItemRequest;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.exception.OrderItemException;
import com.space.munova.order.repository.OrderItemRepository;
import com.space.munova.order.service.cancel.CancellationStrategy;
import com.space.munova.order.service.cancel.OrderCancelStrategy;
import com.space.munova.order.service.cancel.ReturnRefundStrategy;
import com.space.munova.payment.service.PaymentService;
import com.space.munova.product.application.product.command.ProductDetailService;
import com.space.munova.product.domain.ProductDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final ProductDetailService productDetailService;
    private final PaymentService paymentService;
    private final AuthService authService;
    private final OrderCancelStrategy orderCancelStrategy;
    private final ReturnRefundStrategy returnRefundStrategy;

    @Override
    public List<OrderItem> deductStockAndCreateOrderItems(List<OrderItemRequest> itemRequests, Order order) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemRequest orderItemRequest : itemRequests) {
            ProductDetail detail = productDetailService.getStock(orderItemRequest.productDetailId(), orderItemRequest.quantity());

            OrderItem orderItem = OrderItem.create(order, detail, orderItemRequest.quantity());

            orderItems.add(orderItem);
        }

        return orderItems;
    }

    @Transactional
    @Override
    public void cancelOrderItem(Long orderItemId, CancelOrderItemRequest request, Long memberId) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(OrderItemException::notFoundException);

//        authService.verifyAuthorization(orderItem.getOrder().getMember().getId(), memberId);

        CancellationStrategy strategy;
        if (request.cancelType() == CancelType.ORDER_CANCEL) {
            strategy = orderCancelStrategy;
        } else {
            strategy = returnRefundStrategy;
        }

        strategy.validate(orderItem.getStatus());

        paymentService.cancelPaymentAndSaveRefund(orderItem.getId(), orderItem.getOrder().getId(), request);

        productDetailService.increaseStock(orderItem.getProductDetail().getId(), orderItem.getQuantity());

        strategy.updateOrderItemStatus(orderItem);
    }
}
