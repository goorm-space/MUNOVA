package com.space.munova.order.service;

import com.space.munova.auth.exception.AuthException;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.coupon.dto.UseCouponRequest;
import com.space.munova.coupon.dto.UseCouponResponse;
import com.space.munova.coupon.service.CouponService;
import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.order.dto.*;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.entity.OrderProductLog;
import com.space.munova.order.exception.OrderException;
import com.space.munova.order.repository.OrderItemRepository;
import com.space.munova.order.repository.OrderProductLogRepository;
import com.space.munova.order.repository.OrderRepository;
import com.space.munova.payment.entity.Payment;
import com.space.munova.payment.service.PaymentService;
import com.space.munova.product.application.ProductDetailService;
import com.space.munova.recommend.service.RecommendService;
import com.space.munova.security.jwt.JwtHelper;
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

    private final ProductDetailService productDetailService;
    private final CouponService couponService;
    private final OrderItemService orderItemService;
    private final RecommendService recommendService;
    private final PaymentService paymentService;

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final OrderProductLogRepository orderProductLogRepository;


    @Transactional
    @Override
    public Order createOrder(CreateOrderRequest request, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberException::notFoundException);

        // 초기 주문 생성
        Order order = Order.createInitOrder(member, request.userRequest());

        // --- 1. 재고 감소
        List<OrderItem> orderItems = orderItemService.deductStockAndCreateOrderItems(request.orderItems(), order);
        orderItems.forEach(order::addOrderItem);

        // --- 2. 쿠폰 적용
        Order finalOrder = finalizeOrderWithCoupon(order, request);

        orderRepository.save(finalOrder);

        //UserActionSummary 저장 로직
        List<Long> orderItemIds=finalOrder.getOrderItems().stream()
                .map(OrderItem::getId)
                .toList();
        List<Long> productDetailIds=orderItemRepository.findProductDetailIdsByOrderItemIds(orderItemIds);
        for(Long productDetailId:productDetailIds){
            Long productId=productDetailService.findProductIdByDetailId(productDetailId);
            recommendService.updateUserAction(productId,0,null,null,true);
        }
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
    public PagingResponse<OrderSummaryDto> getOrderList(int page, Long memberId) {
        if (page < 0) page = 0;

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
    public GetOrderDetailResponse getOrderDetail(Long orderId) {
        Long userId = JwtHelper.getMemberId();

        Order order = orderRepository.findOrderDetailsById(orderId)
                .orElseThrow(OrderException::notFoundException);

        if (!userId.equals(order.getMember().getId())) {
            throw AuthException.unauthorizedException(
                    "접근 시도한 userId:", userId.toString(),
                    "orderId:", orderId.toString()
            );
        }

        Payment payment = paymentService.getPaymentByOrderId(orderId);

        return GetOrderDetailResponse.from(order, payment);
    }

    public Order finalizeOrderWithCoupon(Order order, CreateOrderRequest request) {
        long totalProductAmount = order.getOrderItems().stream()
                .mapToLong(item -> item.getPriceSnapshot() * item.getQuantity())
                .sum();

        if (request.orderCouponId() != null) {
            UseCouponRequest couponRequest = UseCouponRequest.of(totalProductAmount);
            UseCouponResponse couponResponse = couponService.calculateAmountWithCoupon(request.orderCouponId(), couponRequest);

            if (couponResponse.finalPrice().longValue() != request.clientCalculatedAmount().longValue()) {
                throw OrderException.amountMismatchException(
                        String.format("client: %d, server: %d", request.clientCalculatedAmount(), couponResponse.finalPrice())
                );
            }

            order.updateFinalOrder(
                    couponResponse.originalPrice(),
                    couponResponse.discountPrice(),
                    couponResponse.finalPrice(),
                    request.orderCouponId(),
                    OrderStatus.PAYMENT_PENDING
            );
        } else {
            if (totalProductAmount != request.clientCalculatedAmount()) {
                throw OrderException.amountMismatchException(
                        String.format("client: %d, server: %d", request.clientCalculatedAmount(), totalProductAmount)
                );
            }
            order.updateFinalOrder(
                    totalProductAmount,
                    0L,
                    totalProductAmount,
                    null,
                    OrderStatus.PAYMENT_PENDING
            );
        }

        return order;
    }
}
