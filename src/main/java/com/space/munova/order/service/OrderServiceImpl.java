package com.space.munova.order.service;

import com.space.munova.order.dto.CreateOrderRequest;
import com.space.munova.order.dto.CreateOrderResponse;
import com.space.munova.order.dto.OrderItemRequest;
import com.space.munova.order.dto.OrderType;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.entity.OrderItemStatus;
import com.space.munova.order.entity.OrderStatus;
import com.space.munova.order.repository.OrderItemRepository;
import com.space.munova.order.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
//    private final UserRepository userRepository;
//    private final ProductDetailRepository productDetailRepository;
//    private final CouponRepository couponRepository;
//    private final CartRepository cartRepository;

    @Override
    public Order createOrder(Long userId, CreateOrderRequest request) {

////        User user = userRepository.findById(userId);
//
//        // 1. 주문 헤더 생성 및 초기화
//        Order order = Order.builder()
//                .user(user)
//                .orderNum(Order.generateOrderNum())
//                .userRequest(request.userRequest())
//                .status(OrderStatus.CREATED)
//                .build();
//
//        // 2. 주문 상세 항목 (order Items) 처리
//        List<OrderItem> orderItems = createOrderItems(request.orderItems(), order);
//
//        // 3. 금액 계산 및 쿠폰 적용
//        // 4. 재고 감소
//
//        // 5. 주문 저장
//        orderRepository.save(order);
//        orderItemRepository.saveAll(orderItems);
//
//        // 6. 장바구니 처리
//        if (request.type() == OrderType.CART) {
//            // TODO: 장바구니 삭제
//        }
//
//        return order;
//    }
//
//    private List<OrderItem> createOrderItems(List<OrderItemRequest> itemRequests, Order order) {
//        List<OrderItem> orderItems = new ArrayList<>();
//
//        for(OrderItemRequest itemReq : itemRequests) {
//            ProductDetail detail = productDetailRepository.findById(itemReq.productId())
//                    .orElseThrow(() -> new EntityNotFoundException("ProductDetail not found with Id: " + itemReq.productId()));
//
//            // Todo: 1. 재고 확인 로직
////            if(detail)
//
//            // 2. 쿠폰 조회
//            Coupon itemCoupon = itemReq.couponId() != null
//                    ? couponRepository.findById(itemReq.couponId()).orElse(null)
//                    : null;
//
//            // 3. Orderitem 엔티티 생성
//            OrderItem orderItem = OrderItem.builder()
//                    .order(order)
//                    .productDetailId(detail.getId())
//                    .productName(detail.getProduct().getName())
//                    .originPrice(detail.getProduct().getPrice())
//                    .quantity(itemReq.quantity())
//                    .couponId(itemCoupon != null ? itemCoupon.getId() : null)
////                    .discountPrice() // Todo: 할인금액 계산 로직 작성
////                    .totalPrice()
//                    .status(OrderItemStatus.ORDERED)
//                    .build();
//
//            orderItems.add(orderItem);
//        }
//
//        return orderItems;
        return orderRepository.save(new Order());
    }
}
