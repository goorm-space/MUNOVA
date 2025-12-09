package com.space.munova.order.service;

import com.space.munova.auth.service.AuthService;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.member.entity.Member;
import com.space.munova.member.service.MemberService;
import com.space.munova.order.dto.CreateOrderRequest;
import com.space.munova.order.dto.GetOrderDetailResponse;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.dto.OrderSummaryDto;
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
import com.space.munova.product.application.ProductDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final int PAGE_SIZE = 5;

    private final CouponAppliedProcessor couponAppliedProcessor;
    private final NoCouponProcessor noCouponProcessor;
    private final ProductDetailService productDetailService;
    private final OrderItemService orderItemService;
    private final PaymentService paymentService;
    private final MemberService memberService;
    private final AuthService authService;

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;


    @Transactional
    @Override
    public Order createOrder(CreateOrderRequest request, Long memberId) {
        Member member = memberService.getMemberEntity(memberId);

        // 초기 주문 생성
        Order order = Order.createOrder(member, request.userRequest());

        // 1. 재고 선점
        List<OrderItem> orderItems = orderItemService.deductStockAndCreateOrderItems(request.orderItems(), order);
        orderItems.forEach(order::addOrderItem);

        // 2. 총액 계산
        long totalProductAmount = order.getOrderItems().stream()
                .mapToLong(OrderItem::calculateAmount)
                .sum();

        // 3. 쿠폰 유무에 따라 금액 계산
        OrderAmountProcessor processor;
        if (request.orderCouponId() != null) {
            processor = couponAppliedProcessor;
        } else {
            processor = noCouponProcessor;
        }

        processor.process(order, request, totalProductAmount);

        orderRepository.save(order);
        return order;
    }

    @Transactional(readOnly = false)
    @Override
    public void saveOrderLog(Order order) {
        // 주문 로그는 Redis Stream으로 전송 (추천 서버 파이프라인)
        // DB 테이블 저장 제거됨 - order_product_log 테이블 사용 안 함
    }

    @Override
    public PagingResponse<OrderSummaryDto> getOrderList(int page, Long memberId) {
        Pageable pageable = PageRequest.of(
                page,
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Order> orderPage = orderRepository.findAllByMember_IdAndStatus(memberId, OrderStatus.PAID, pageable);

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
    public GetOrderDetailResponse getOrderDetail(Long orderId, Long memberId) {

        Order order = orderRepository.findOrderDetailsById(orderId)
                .orElseThrow(OrderException::notFoundException);

        authService.verifyAuthorization(order.getMember().getId(), memberId);

        Payment payment = paymentService.getPaymentByOrderId(orderId);

        return GetOrderDetailResponse.from(order, payment);
    }
}
