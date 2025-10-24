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
    public Order createOrder(Long userId, CreateOrderRequest request) {

        Member member = memberRepository.findById(userId)
                .orElseThrow(MemberException::notFoundException);

        // 1. 주문 생성 및 초기화
        Order order = Order.builder()
                .member(member)
                .orderNum(Order.generateOrderNum())
                .userRequest(request.userRequest())
                .status(OrderStatus.PAYMENT_PENDING)
                .build();

        // 2. 주문 상품 재고 확인
        List<OrderItem> orderItems = createOrderItems(request.orderItems(), order);

        // Todo: 3. 쿠폰 적용 및 금액 계산
        long originPrice = orderItems.stream()
                .mapToLong(item -> item.getPrice() * item.getQuantity())
                .sum();
//        Coupon coupon = couponRepository.findById(request.orderCouponId())
//                .orElseThrow(CouponException::notFoundException);
        int discountPrice = 0;
        Long totalPrice = originPrice - discountPrice;
        order.setPrices(originPrice, discountPrice, totalPrice);

        // Todo: 4. 결제 및 status 변경, 재고 감소

        // 5. 주문 저장
        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);

        // Todo: 결제정보 저장

        // Todo: 6. 장바구니 삭제
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
    public GetOrderDetailResponse getOrderDetail(Long userId, Long orderId) {
        Order order = orderRepository.findOrderDetailsById(orderId)
                .orElseThrow(OrderException::notFoundException);

        if (!userId.equals(order.getMember().getId())) {
            throw AuthException.unauthorizedException(
                    "접근 시도한 userId:", userId.toString(),
                    "orderId:", orderId.toString()
            );
        }
        return GetOrderDetailResponse.from(order);
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
                    .productName(detail.getProduct().getName())
                    .price(detail.getProduct().getPrice())
                    .quantity(itemReq.quantity())
                    .status(OrderStatus.PAYMENT_PENDING)
                    .build();

            orderItems.add(orderItem);
        }

        return orderItems;
    }
}
