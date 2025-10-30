package com.space.munova.order.service;

import com.space.munova.auth.exception.AuthException;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.coupon.dto.UseCouponRequest;
import com.space.munova.coupon.dto.UseCouponResponse;
import com.space.munova.coupon.entity.Coupon;
import com.space.munova.coupon.exception.CouponException;
import com.space.munova.coupon.repository.CouponRepository;
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
import com.space.munova.product.application.ProductDetailService;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.recommend.service.RecommendService;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Or;
import org.springframework.data.domain.*;
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
    private final CouponService couponService;

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final RecommendService recommendService;

    private final OrderProductLogRepository orderProductLogRepository;
    private final CouponRepository couponRepository;

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
    public Page<OrderSummaryDto> getOrdersByMember(Long memberId, OrderStatus status, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findAllByMember_IdAndStatus(memberId, status, pageable);

        if (orderPage.getContent().isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> orderIds = orderPage.getContent().stream()
                .map(Order::getId)
                .toList();

        List<Order> ordersWithDetails = orderRepository.findAllWithDetailsByOrderIds(orderIds);

        List<OrderSummaryDto> orderDtos = ordersWithDetails.stream()
                .map(OrderSummaryDto::from)
                .toList();

        return new PageImpl<>(orderDtos, pageable, orderPage.getTotalElements());
    }

    @Override
    public PagingResponse<OrderSummaryDto> getOrderList(int page) {
        Long userId = JwtHelper.getMemberId();
        Pageable pageable = PageRequest.of(
                page,
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<OrderSummaryDto> orders = getOrdersByMember(userId, OrderStatus.PAID, pageable);
        return PagingResponse.from(orders);
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

        if (request.orderCouponId() != null) {
            UseCouponRequest couponRequest = UseCouponRequest.of(totalProductAmount);
            UseCouponResponse couponResponse = couponService.useCoupon(request.orderCouponId(), couponRequest);

            if (couponResponse.finalPrice().longValue() != request.clientCalculatedAmount().longValue()) {
                throw OrderException.amountMismatchException(
                        String.format("client: %d, server: %d", request.clientCalculatedAmount(), couponResponse.finalPrice())
                );
            }

            Coupon coupon = couponRepository.findWithCouponDetailById(request.orderCouponId())
                    .orElseThrow(CouponException::notFoundException);

            order.updateFinalOrder(
                    couponResponse.originalPrice(),
                    couponResponse.discountPrice(),
                    couponResponse.finalPrice(),
                    coupon,
                    OrderStatus.PAYMENT_PENDING
            );
        } else {
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
