package com.space.munova.order.service;

import com.space.munova.auth.exception.AuthException;
import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.order.dto.*;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.exception.OrderException;
import com.space.munova.order.repository.OrderItemRepository;
import com.space.munova.order.repository.OrderRepository;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.product.domain.Repository.ProductDetailRepository;
import com.space.munova.product.exception.ProductDetailException;
import com.space.munova.product.exception.ProductException;
import com.space.munova.security.jwt.JwtHelper;
import jakarta.persistence.EntityNotFoundException;
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

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MemberRepository memberRepository;
    private final ProductDetailRepository productDetailRepository;

    @Transactional
    @Override
    public Order createTmpOrder(CreateOrderRequest request) {
        Long userId = JwtHelper.getMemberId();
        Member member = memberRepository.findById(userId)
                .orElseThrow(MemberException::notFoundException);

        Order order = Order.builder()
                .member(member)
                .orderNum(Order.generateOrderNum())
                .status(OrderStatus.CREATED)
                .build();

        // 2. 주문 상품 재고 확인
        List<OrderItem> orderItems = createOrderItems(request.orderItems(), order);
        orderItems.forEach(order::addOrderItem);

        orderRepository.save(order);

        return order;
    }

    @Transactional
    @Override
    public Order confirmOrder(ConfirmOrderRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(OrderException::notFoundException);

        CalculatedAmounts amounts = calculateFinalAmount(order, request.orderCouponId());

        if (!amounts.finalAmount().equals(request.clientCalculatedAmount())) {
            throw OrderException.amountMismatchException(
                    "client: " + request.clientCalculatedAmount() + ", server: " + amounts.finalAmount()
            );
        }

        order.updateFinalOrder(
                amounts.productAmount(),
                amounts.discountAmount(),
                amounts.finalAmount(),
                request.orderCouponId(),
                request.userRequest(),
                OrderStatus.PAYMENT_PENDING
        );

        return order;
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

    private List<OrderItem> createOrderItems(List<OrderItemRequest> itemRequests, Order order) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemReq : itemRequests) {
            ProductDetail detail = productDetailRepository.findById(itemReq.productId())
                    .orElseThrow(ProductDetailException::notFoundException);

            // 1. 재고 확인
            if (detail.getQuantity() == 0) {
                throw ProductDetailException.noStockException("product_detail_id: " + itemReq.productId());
            } else if (detail.getQuantity() < itemReq.quantity()) {
                throw ProductDetailException.stockInsufficientException("product_detail_id: " + itemReq.productId() + ", 요청: " + itemReq.quantity() + ", 재고: " + detail.getQuantity());
            }

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

    private CalculatedAmounts calculateFinalAmount(Order order, Long orderCouponId) {
        long totalProductAmount = order.getOrderItems().stream()
                .mapToLong(item -> item.getPriceSnapshot() * item.getQuantity())
                .sum();
        // Todo: 쿠폰id를 사용해서 할인액 계산하는 로직 필요
        int discountAmount = 5000;
        long finalAmount = totalProductAmount - discountAmount;

        return new CalculatedAmounts(totalProductAmount, discountAmount, finalAmount);
    }
}
