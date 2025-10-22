package com.space.munova.order.service;

import com.space.munova.member.entity.Member;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.order.dto.*;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.entity.OrderStatus;
import com.space.munova.order.repository.OrderItemRepository;
import com.space.munova.order.repository.OrderRepository;
import com.space.munova.product.domain.product.Jpa.JpaProductDetailRepository;
import com.space.munova.product.domain.product.ProductDetail;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final int PAGE_SIZE = 5;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MemberRepository memberRepository;
    private final JpaProductDetailRepository productDetailRepository;
//    private final CouponRepository couponRepository;
//    private final CartRepository cartRepository;

    @Override
    public Order createOrder(Long userId, CreateOrderRequest request) {

        Member member = memberRepository.findById(userId)
                .orElseThrow(EntityNotFoundException::new);

        // 1. 주문 헤더 생성 및 초기화
        Order order = Order.builder()
                .member(member)
                .orderNum(Order.generateOrderNum())
                .userRequest(request.userRequest())
                .status(OrderStatus.PAYMENT_PENDING)
                .build();

        // 2. 주문 상세 항목 (order Items) 처리
        List<OrderItem> orderItems = createOrderItems(request.orderItems(), order);

        // Todo: 3. 쿠폰 적용 및 금액 계산
        long originPrice = orderItems.stream()
                .mapToLong(item -> item.getOriginPrice() * item.getQuantity())
                        .sum();
        int discountPrice = 0;
        Long totalPrice = originPrice - discountPrice;
        order.setPrices(originPrice, discountPrice, totalPrice);

        // Todo: 4. 재고 감소

        // 5. 주문 저장
        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);

        // Todo: 6. 장바구니 처리
        return order;
    }

    @Override
    public GetOrderListResponse getOrderList(int page) {
        Pageable pageable = PageRequest.of(
                page,
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Order> orderPage = orderRepository.findAll(pageable);

        Page<OrderSummaryDto> dtoPage = orderPage.map(OrderSummaryDto::from);

        return GetOrderListResponse.from(dtoPage);
    }

    @Override
    public GetOrderDetailResponse getOrderDetail(Long orderId) {

        Order order = orderRepository.findOrderDetailsById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 orderId: "+ orderId));

        return GetOrderDetailResponse.from(order);
    }

    private List<OrderItem> createOrderItems(List<OrderItemRequest> itemRequests, Order order) {
        List<OrderItem> orderItems = new ArrayList<>();

        for(OrderItemRequest itemReq : itemRequests) {
            ProductDetail detail = productDetailRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new EntityNotFoundException("ProductDetail not found with Id: " + itemReq.productId()));

            // Todo: 1. 재고 확인 로직 작성 -> 결제 단계에서 확인하는지 여부??
//            detail.decreaseQuantity(itemReq.quantity());
//            productDetailRepository.save(detail);

            // 2. Orderitem 엔티티 생성
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productDetail(detail)
                    .productName(detail.getProduct().getName())
                    .originPrice(detail.getProduct().getPrice())
                    .quantity(itemReq.quantity())
                    .status(OrderStatus.PAYMENT_PENDING)
                    .build();

            orderItems.add(orderItem);
        }

        return orderItems;
    }
}
