package com.space.munova.order.service;

import com.space.munova.auth.exception.AuthException;
import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.order.dto.*;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.entity.OrderProductLog;
import com.space.munova.order.exception.OrderException;
import com.space.munova.order.repository.OrderProductLogRepository;
import com.space.munova.order.repository.OrderRepository;
import com.space.munova.product.application.ProductDetailService;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final int PAGE_SIZE = 5;

    private final ProductDetailService productDetailService;

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;

    private final OrderProductLogRepository orderProductLogRepository;

    @Transactional
    @Override
    public Order createOrder(CreateOrderRequest request) {
        Long userId = JwtHelper.getMemberId();
        Member member = memberRepository.findById(userId)
                .orElseThrow(MemberException::notFoundException);

        // 초기 주문 생성
        Order order = Order.builder()
                .member(member)
                .orderNum(Order.generateOrderNum())
                .userRequest(request.userRequest())
                .status(OrderStatus.CREATED)
                .build();

        // --- 1. 재고 감소
        List<OrderItem> orderItems = deductStockAndCreateOrderItems(request.orderItems(), order);
        orderItems.forEach(order::addOrderItem);

        // --- 2. 쿠폰 적용
        Order finalOrder = applyCouponAndOrder(order, request);

        orderRepository.save(finalOrder);

        return finalOrder;
    }

    @Transactional(readOnly = false)
    @Override
    public void saveOrderLog(Order order){
        Long memberId = order.getMember().getId();
        for(OrderItem item : order.getOrderItems()) {
            Long productId=item.getProductDetail().getProduct().getId();
            Integer quantity=item.getQuantity();
            OrderProductLog log=OrderProductLog.builder()
                    .memberId(memberId)
                    .productId(productId)
                    .quantity(quantity)
                    .price(item.getPriceSnapshot())
                    .orderStatus(item.getStatus())
                    .build();
            orderProductLogRepository.save(log);
        }
    }

    @Override
    public GetOrderListResponse getOrderList(Long userId, int page) {
        Pageable pageable = PageRequest.of(
                page,
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Order> orderPage = orderRepository.findAllByMember_Id(userId, pageable);

        Page<OrderSummaryDto> dtoPage = orderPage.map(OrderSummaryDto::from);

        return GetOrderListResponse.from(dtoPage);
    }

    @Override
    public Order getOrderDetail(Long orderId) {
        Long userId = JwtHelper.getMemberId();

        Order order = orderRepository.findOrderDetailsById(orderId)
                .orElseThrow(OrderException::notFoundException);

        if (!userId.equals(order.getMember().getId())) {
            throw AuthException.unauthorizedException(
                    "접근 시도한 userId:", userId.toString(),
                    "orderId:", orderId.toString()
            );
        }
        return order;
    }

    private List<OrderItem> deductStockAndCreateOrderItems(List<OrderItemRequest> itemRequests, Order order) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemReq : itemRequests) {
            ProductDetail detail = productDetailService.deductStock(itemReq.productDetailId(), itemReq.quantity());

            // 2. Orderitem 엔티티 생성
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productDetail(detail)
                    .nameSnapshot(detail.getProduct().getName())
                    .priceSnapshot(detail.getProduct().getPrice())
                    .quantity(itemReq.quantity())
                    .status(OrderStatus.CREATED)
                    .build();

            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private Order applyCouponAndOrder(Order order, CreateOrderRequest request) {
        long totalProductAmount = order.getOrderItems().stream()
                .mapToLong(item -> item.getPriceSnapshot() * item.getQuantity())
                .sum();
        // Todo: 쿠폰id를 사용해서 할인액 계산하는 로직 필요
        int discountAmount = 5000;
        long finalAmount = totalProductAmount - discountAmount;

        if (finalAmount != request.clientCalculatedAmount()) {
            throw OrderException.amountMismatchException(
                    String.format("client: %d, server: %d", request.clientCalculatedAmount(), finalAmount)
            );
        }

        order.updateFinalOrder(
                totalProductAmount,
                discountAmount,
                finalAmount,
                request.orderCouponId(),
                OrderStatus.PAYMENT_PENDING
        );

        return order;
    }
}
