package com.space.munova.order.service;

import com.space.munova.auth.service.AuthService;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.member.service.MemberService;
import com.space.munova.order.dto.*;
import com.space.munova.order.dto.redis.TmpOrderDto;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.entity.OrderProductLog;
import com.space.munova.order.exception.OrderException;
import com.space.munova.order.repository.OrderItemRepository;
import com.space.munova.order.repository.OrderProductLogRepository;
import com.space.munova.order.repository.OrderRepository;
import com.space.munova.order.service.processor.CouponAppliedProcessor;
import com.space.munova.order.service.processor.NoCouponProcessor;
import com.space.munova.order.service.processor.OrderAmountProcessor;
import com.space.munova.payment.entity.Payment;
import com.space.munova.payment.service.PaymentService;
import com.space.munova.product.application.ProductDetailService;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
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

    private final CouponAppliedProcessor couponAppliedProcessor;
    private final NoCouponProcessor noCouponProcessor;
    private final ProductDetailService productDetailService;
    private final OrderItemService orderItemService;
    private final RecommendService recommendService;
    private final PaymentService paymentService;
    private final MemberService memberService;
    private final AuthService authService;
    private final RedisService redisService;

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final OrderProductLogRepository orderProductLogRepository;

    @Override
    public Order saveTmpOrder(CreateOrderRequest request, Long memberId) {
        // 1. 재고 검증 후 선점
        redisService.validateAndDecreaseStock(request.orderItems());

        // 2. 초기 주문 생성
        Order order = Order.createOrder(request.userRequest());

        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemRequest orderItemRequest : request.orderItems()) {
            ProductDetail detail = productDetailService.findById(orderItemRequest.productDetailId());

            OrderItem orderItem = OrderItem.create(order, detail, orderItemRequest.quantity());
            orderItems.add(orderItem);
        }
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

        TmpOrderDto tmpOrder = TmpOrderDto.from(memberId, order);
        redisService.saveTemporaryOrder(tmpOrder);

        // Todo: rdb에 저장 후에 불러와야 합니다
        // UserActionSummary 저장 로직
        saveRecommendFromOrder(order);

        return order;
    }

    @Transactional
    public void saveRecommendFromOrder(Order order) {
        List<Long> orderItemIds = order.getOrderItems().stream()
                .map(OrderItem::getId)
                .toList();
        List<Long> productDetailIds = orderItemRepository.findProductDetailIdsByOrderItemIds(orderItemIds);
        for (Long productDetailId : productDetailIds) {
            Long productId = productDetailService.findProductIdByDetailId(productDetailId);
            recommendService.updateUserAction(productId, 0, null, null, true);
        }
    }

    @Transactional
    @Override
    public void saveOrderLog(Long memberId, Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Long productId = item.getProductDetail().getProduct().getId();
            Integer quantity = item.getQuantity();
            OrderProductLog log = OrderProductLog.builder()
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

    @Transactional
    @Override
    public void saveOrder(Order order) {
        orderRepository.save(order);
        redisService.deleteTmpOrder(order.getOrderNum());
    }
}
