package com.space.munova.order.service;

import com.space.munova.auth.service.AuthService;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.order.dto.*;
import com.space.munova.order.dto.redis.TmpOrderDto;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.exception.OrderException;
import com.space.munova.order.repository.OrderItemRepository;
import com.space.munova.order.repository.OrderRepository;
import com.space.munova.order.service.processor.CouponAppliedProcessor;
import com.space.munova.order.service.processor.NoCouponProcessor;
import com.space.munova.order.service.processor.OrderAmountProcessor;
import com.space.munova.payment.entity.Payment;
import com.space.munova.payment.service.PaymentService;
import com.space.munova.product.application.product.command.ProductDetailService;
import com.space.munova.product.domain.ProductDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final int PAGE_SIZE = 5;

    private final CouponAppliedProcessor couponAppliedProcessor;
    private final NoCouponProcessor noCouponProcessor;
    private final ProductDetailService productDetailService;
    private final AsyncOrderService asyncOrderService;
    private final OrderItemService orderItemService;
    private final PaymentService paymentService;
    private final AuthService authService;
    private final RedisService redisService;

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public Order saveTmpOrder(CreateOrderRequest request, Long memberId) {
        // 1. 재고 검증
        for (OrderItemRequest orderItem : request.orderItems()) {
            redisService.validateStock(orderItem.productDetailId(), orderItem.quantity());
        }

        // 2. 초기 주문 생성
        Order order = Order.createOrder(request.userRequest(), memberId);
        List<OrderItem> orderItems = orderItemService.createOrderItems(request.orderItems(), order);
        orderItems.forEach(order::addOrderItem);

        // 3. 총액 계산
        long totalProductAmount = order.getOrderItems().stream()
                .mapToLong(OrderItem::calculateAmount)
                .sum();

        // 4. 쿠폰 유무에 따라 금액 계산
        OrderAmountProcessor processor;
        if (request.orderCouponId() != null) {
            processor = couponAppliedProcessor;
        } else {
            processor = noCouponProcessor;
        }
        processor.process(order, request, totalProductAmount);

        TmpOrderDto tmpOrder = TmpOrderDto.from(memberId, order);
        redisService.saveTemporaryOrder(tmpOrder);

        orderRepository.save(order);
//        asyncOrderService.saveOrderAsync(order);

        return order;
    }

    @Override
    public PagingResponse<OrderSummaryDto> getOrderList(int page, Long memberId) {
        Pageable pageable = PageRequest.of(
                page,
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Order> orderPage = orderRepository.findAllByMemberIdAndStatus(memberId, OrderStatus.PAID, pageable);

        if (orderPage.getContent().isEmpty()) {
            return PagingResponse.from(Page.empty(pageable));
        }

        List<Long> orderIds = orderPage.getContent().stream()
                .map(Order::getId)
                .toList();

        List<Order> ordersWithDetails = orderRepository.findAllWithDetailsByOrderIds(orderIds);

        List<OrderSummaryDto> orderDtos = ordersWithDetails.stream()
                .map(OrderSummaryDto::from)
                .toList();

        return PagingResponse.from(new PageImpl<>(orderDtos, pageable, orderPage.getTotalElements()));
    }

    @Override
    public GetOrderDetailResponse getOrderDetail(Long orderId, Long memberId, String username, String address) {

        Order order = orderRepository.findOrderDetailsById(orderId)
                .orElseThrow(OrderException::notFoundException);

        authService.verifyAuthorization(order.getMemberId(), memberId);

        Payment payment = paymentService.getPaymentByOrderId(orderId);

        return GetOrderDetailResponse.from(order, payment, username, address);
    }

    @Override
    public Order getOrderByOrderNum(String orderNum) {
        return orderRepository.findByOrderNum(orderNum)
                .orElseThrow(OrderException::notFoundException);
    }

    @Override
    public Order getOrderWithItems(String orderNum) {
        return orderRepository.findByOrderNumWithItems(orderNum)
                .orElseThrow(OrderException::notFoundException);
    }
}
