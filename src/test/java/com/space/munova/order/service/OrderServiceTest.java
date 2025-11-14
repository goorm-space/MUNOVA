package com.space.munova.order.service;

import com.space.munova.auth.exception.AuthException;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.coupon.dto.UseCouponRequest;
import com.space.munova.coupon.dto.UseCouponResponse;
import com.space.munova.coupon.service.CouponService;
import com.space.munova.member.entity.Member;
import com.space.munova.order.dto.*;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.exception.OrderException;
import com.space.munova.order.repository.OrderRepository;
import com.space.munova.payment.entity.Payment;
import com.space.munova.payment.entity.PaymentMethod;
import com.space.munova.payment.exception.PaymentException;
import com.space.munova.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


@DisplayName("Order_Service")
@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private CouponService couponService;
    @Mock
    private PaymentService paymentService;

    @Mock
    private OrderRepository orderRepository;

    private Member member;
    private CreateOrderRequest createOrderRequest;
    private Order initOrderWithItems;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(1L).build();
        createOrderRequest = new CreateOrderRequest(
                1L,
                "문 앞에 배송해주세요",
                9000L,
                List.of(new OrderItemRequest(1L, 1))
        );
        initOrderWithItems = Order.createInitOrder(member, null);
        initOrderWithItems.getOrderItems().add(OrderItem.builder().priceSnapshot(10000L).quantity(1).build());
    }

    @DisplayName("[주문서 초기 생성] (HappyCase) 사용자가 작성한 주문서로 초기 주문서를 생성한다.")
    @Test
    void createInitOrder_happyCase() {
        // given
        String userRequest = "문 앞에 배송해주세요";

        // when
        Order initOrder = Order.createInitOrder(member, userRequest);

        // then
        assertThat(initOrder).isNotNull();
        assertThat(initOrder.getMember()).isSameAs(member);
        assertThat(initOrder.getOrderNum()).isNotBlank();
        assertThat(initOrder.getOrderNum()).matches("^[0-9]{8}[A-Z0-9]{8}$");
        assertThat(initOrder.getUserRequest()).isEqualTo(userRequest);
        assertThat(initOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(initOrder.getOrderItems()).isEmpty();
        assertThat(initOrder.getCouponId()).isNull();
        assertThat(initOrder.getOriginPrice()).isNull();
        assertThat(initOrder.getDiscountPrice()).isNull();
        assertThat(initOrder.getTotalPrice()).isNull();
    }

    @DisplayName("[주문서 초기 생성] 배송 요청사항(userRequest)가 null이어도 초기 주문을 생성할 수 있다.")
    @Test
    void createInitOrder_nullUserRequest() {
        // given

        // when
        Order initOrder = Order.createInitOrder(member, null);

        // then
        assertThat(initOrder.getUserRequest()).isNull();
        assertThat(initOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @DisplayName("[주문서 생성] (HappyCase) 쿠폰 적용 시 서버가 계산한 예상금액이 client 예상금액과 같으면 주문서를 확정짓는다.")
    @Test
    void finalizeOrder_withCoupon_happyCase() {
        // given
        UseCouponResponse response = UseCouponResponse.of(10000L,1000L, 9000L);
        when(couponService.calculateAmountWithCoupon(eq(createOrderRequest.orderCouponId()), any(UseCouponRequest.class))).thenReturn(response);

        // when
        Order result = orderService.finalizeOrderWithCoupon(initOrderWithItems, createOrderRequest);

        // then
        assertThat(result.getTotalPrice()).isEqualTo(createOrderRequest.clientCalculatedAmount());
        assertThat(result.getCouponId()).isEqualTo(createOrderRequest.orderCouponId());
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);

        verify(couponService, times(1)).calculateAmountWithCoupon(1L, new UseCouponRequest(10000L));
    }

    @DisplayName("[주문서 생성] 쿠폰 적용 시 서버가 계산한 예상금액이 client 예상금액과 다르면 예외를 던진다.")
    @Test
    void finalizeOrder_withCoupon_amountMismatch_throws() {
        // given
        UseCouponResponse response = UseCouponResponse.of(10000L,0L, 10000L);
        when(couponService.calculateAmountWithCoupon(eq(createOrderRequest.orderCouponId()), any(UseCouponRequest.class))).thenReturn(response);

        // when

        // then
        assertThatThrownBy(() -> orderService.finalizeOrderWithCoupon(initOrderWithItems, createOrderRequest))
                .isInstanceOf(OrderException.class);
    }

    @DisplayName("[주문서 생성] (HappyCase) 쿠폰 적용을 안해도 주문서를 확정 지을 수 있다.")
    @Test
    void finalizeOrder_withoutCoupon_happyCase() {
        // given
        CreateOrderRequest request = new CreateOrderRequest(null, null, 10000L, null);

        // when
        Order result = orderService.finalizeOrderWithCoupon(initOrderWithItems, request);

        // then
        assertThat(result.getTotalPrice()).isEqualTo(request.clientCalculatedAmount());
        assertThat(result.getCouponId()).isNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @DisplayName("[주문서 생성] 쿠폰 적용 안할 시 서버와 client 예상 금액이 다르면 예외를 던진다.")
    @Test
    void finalizeOrder_withoutCoupon_amountMismatch_throws() {
        // given
        CreateOrderRequest request = new CreateOrderRequest(null, null, 9000L, null);

        // when

        // then
        assertThatThrownBy(() -> orderService.finalizeOrderWithCoupon(initOrderWithItems, request))
                .isInstanceOf(OrderException.class);
    }

    @DisplayName("[전체 조회] (HappyCase) 사용자가 결제한 주문 내역들을 조회한다.")
    @Test
    void getOrderList_whenOrdersExist_happyCase() {
        // given
        Long memberId = 1L;
        OrderStatus status = OrderStatus.PAID;
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

        Order o1 = Order.builder().build();
        Order o2 = Order.builder().build();

        Order spy1 = spy(o1);
        Order spy2 = spy(o2);
        when(spy1.getId()).thenReturn(1L);
        when(spy2.getId()).thenReturn(2L);

        Page<Order> firstPage = new PageImpl<>(List.of(spy1, spy2), pageable, 50L);
        when(orderRepository.findAllByMember_IdAndStatus(anyLong(), any(OrderStatus.class), any(Pageable.class)))
                .thenReturn(firstPage);

        when(orderRepository.findAllWithDetailsByOrderIds(anyList()))
                .thenReturn(List.of(spy1, spy2));

        OrderSummaryDto dto1 = new OrderSummaryDto(1L, LocalDateTime.now(), List.of(OrderItemDto.builder().build()));
        OrderSummaryDto dto2 = new OrderSummaryDto(2L, LocalDateTime.now(), List.of(OrderItemDto.builder().build()));
        try (MockedStatic<OrderSummaryDto> mocked = mockStatic(OrderSummaryDto.class)) {
            mocked.when(() -> OrderSummaryDto.from(spy1)).thenReturn(dto1);
            mocked.when(() -> OrderSummaryDto.from(spy2)).thenReturn(dto2);

            // when
            PagingResponse<OrderSummaryDto> result = orderService.getOrderList(0, memberId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(2);
            assertThat(result.content()).containsExactly(dto1, dto2);
            assertThat(result.totalElements()).isEqualTo(50L);

            verify(orderRepository, times(1)).findAllByMember_IdAndStatus(memberId, status, pageable);
            verify(orderRepository, times(1)).findAllWithDetailsByOrderIds(List.of(1L, 2L));
        }
    }

    @DisplayName("[전체 조회] (HappyCase) 주문 내역이 없으면 빈 페이지를 반환한다.")
    @Test
    void getOrderList_whenNoOrders_happyCase() {
        // given
        Long memberId = 1L;
        OrderStatus status = OrderStatus.PAID;
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

        when(orderRepository.findAllByMember_IdAndStatus(anyLong(), any(OrderStatus.class), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        // when
        PagingResponse<OrderSummaryDto> result = orderService.getOrderList(0, memberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0L);

        verify(orderRepository, times(1)).findAllByMember_IdAndStatus(memberId, status, pageable);
        verify(orderRepository, never()).findAllWithDetailsByOrderIds(anyList());
    }

    @DisplayName("[상세 조회] (HappyCase) 주문내역 존재 + 사용자 일치하면, 주문 상세 내역을 조회할 수 있다.")
    @Test
    void getOrderDetail_happyCase() {
        // given
        Long memberId = 1L;
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .member(member)
                .totalPrice(15000L)
                .status(OrderStatus.PAID)
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .orderId(orderId)
                .method(PaymentMethod.카드)
                .build();

        when(orderRepository.findOrderDetailsById(orderId)).thenReturn(Optional.of(order));
        when(paymentService.getPaymentByOrderId(orderId)).thenReturn(payment);

        // when
        GetOrderDetailResponse resp = orderService.getOrderDetail(orderId, memberId);

        // then
        assertThat(member.getId()).isEqualTo(memberId);
        assertThat(resp).isNotNull();
        assertThat(resp.orderId()).isEqualTo(orderId);
        assertThat(resp.totalPrice()).isEqualTo(order.getTotalPrice());
        assertThat(resp.paymentMethod()).isEqualTo(payment.getMethod());

        verify(orderRepository, times(1)).findOrderDetailsById(orderId);
        verify(paymentService, times(1)).getPaymentByOrderId(orderId);
    }

    @DisplayName("[상세 조회] orderId에 해당하는 주문이 없으면 예외를 발생한다.")
    @Test
    void getOrderDetail_whenOrderNotFound_throwsOrderNotFound() {
        // given
        Long orderId = 100L;
        when(orderRepository.findOrderDetailsById(orderId)).thenReturn(Optional.empty());

        // when

        // then
        assertThatThrownBy(() -> orderService.getOrderDetail(orderId, member.getId()))
                .isInstanceOf(OrderException.class);

        verify(orderRepository, times(1)).findOrderDetailsById(orderId);
        verifyNoInteractions(paymentService);
    }

    @DisplayName("[상세 조회] 요청한 사용자가 주문 소유자가 아니면 예외를 발생한다.")
    @Test
    void getOrderDetail_whenNotOwner_throwsAuthException() {
        // given
        Long orderId = 1L;
        Long requestMemberId = 100L;

        Order order = Order.builder().id(orderId).member(member).build();

        when(orderRepository.findOrderDetailsById(orderId)).thenReturn(Optional.of(order));

        // when

        // then
        assertThatThrownBy(() -> orderService.getOrderDetail(orderId, requestMemberId))
                .isInstanceOf(AuthException.class);

        verify(orderRepository, times(1)).findOrderDetailsById(orderId);
        verifyNoInteractions(paymentService);
    }

    @DisplayName("[상세 조회] orderId에 해당하는 결제내역이 없으면 예외를 발생한다.")
    @Test
    void getOrderDetail_whenPaymentServiceThrows_propagatesException() {
        // given
        Long orderId = 1L;

        Order order = Order.builder().id(orderId).member(member).build();

        when(orderRepository.findOrderDetailsById(orderId)).thenReturn(Optional.of(order));
        when(paymentService.getPaymentByOrderId(orderId)).thenThrow(PaymentException.class);

        // when

        // then
        assertThatThrownBy(() -> orderService.getOrderDetail(orderId, member.getId()))
                .isInstanceOf(PaymentException.class);

        verify(orderRepository, times(1)).findOrderDetailsById(orderId);
        verify(paymentService, times(1)).getPaymentByOrderId(orderId);
    }

}
