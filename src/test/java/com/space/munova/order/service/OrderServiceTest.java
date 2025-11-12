package com.space.munova.order.service;

import com.space.munova.coupon.dto.UseCouponRequest;
import com.space.munova.coupon.dto.UseCouponResponse;
import com.space.munova.coupon.service.CouponService;
import com.space.munova.member.entity.Member;
import com.space.munova.order.dto.CreateOrderRequest;
import com.space.munova.order.dto.OrderItemRequest;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.exception.OrderException;
import com.space.munova.order.exception.OrderItemException;
import com.space.munova.product.application.ProductDetailService;
import com.space.munova.product.application.exception.ProductDetailException;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.ProductDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderItemService orderItemService;
    @Mock
    private ProductDetailService productDetailService;
    @Mock
    private CouponService couponService;

    private Member member;
    private CreateOrderRequest createOrderRequest;
    private OrderItemRequest req;
    private Order initOrder;
    private ProductDetail productDetail;
    private Order initOrderWithItems;

    @BeforeEach
    void setUp() {
        member = mock(Member.class);
        createOrderRequest = new CreateOrderRequest(
                1L,
                "문 앞에 배송해주세요",
                9000L,
                List.of(new OrderItemRequest(1L, 1))
        );
        req = new OrderItemRequest(1L, 2);
        initOrder = Order.createInitOrder(member, null);
        initOrderWithItems = Order.createInitOrder(member, null);
        initOrderWithItems.getOrderItems().add(OrderItem.builder().priceSnapshot(10000L).quantity(1).build());


        Product product = Product.builder()
                .id(1L)
                .name("상품A")
                .price(10000L)
                .build();
        productDetail = ProductDetail.builder()
                .id(1L)
                .product(product)
                .quantity(10)
                .build();
    }

    @DisplayName("사용자가 작성한 주문서로 초기 주문서를 생성한다.")
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

    @DisplayName("배송 요청사항(userRequest)가 null이어도 초기 주문을 생성할 수 있다.")
    @Test
    void createInitOrder_nullUserRequest() {
        // given

        // when
        Order initOrder = Order.createInitOrder(member, null);

        // then
        assertThat(initOrder.getUserRequest()).isNull();
        assertThat(initOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @DisplayName("한 개의 주문 요청 상품에 대해 주문상품 엔티티(orderItem)을 생성한다.")
    @Test
    void createOrderItems_singleItem_HappyCase() {
        // given
        Order order = Order.builder().build();

        when(productDetailService.deductStock(anyLong(), anyInt())).thenReturn(productDetail);

        // when
        List<OrderItem> items = orderItemService.deductStockAndCreateOrderItems(List.of(req), order);

        // then
        assertThat(items).hasSize(1);
        OrderItem orderItem = items.get(0);
        assertThat(orderItem.getOrder()).isSameAs(order);
        assertThat(orderItem.getProductDetail()).isSameAs(productDetail);
        assertThat(orderItem.getNameSnapshot()).isEqualTo("상품A");
        assertThat(orderItem.getPriceSnapshot()).isEqualTo(10000L);
        assertThat(orderItem.getQuantity()).isEqualTo(2);
        assertThat(orderItem.getStatus()).isEqualTo(OrderStatus.CREATED);

        verify(productDetailService, times(1)).deductStock(1L, 2);
    }

    @DisplayName("orderItem을 생성할 때 재고가 없으면 noStockException 예외를 던진다.")
    @Test
    void createOrderItems_noStock_throws() {
        // given
        Order order = Order.createInitOrder(member, null);

        // when
        when(productDetailService.deductStock(anyLong(), anyInt()))
                .thenThrow(ProductDetailException.noStockException());

        // then
        assertThatThrownBy(() -> orderItemService.deductStockAndCreateOrderItems(List.of(req), order))
                .isInstanceOf(ProductDetailException.class)
                .hasMessageContaining("재고가 없습니다.");

        verify(productDetailService).deductStock(1L, 2);
    }

    @DisplayName("orderItem을 생성할 때 주문 요청 상품이 없으면 noOrderItemsNotAllowedException 예외를 던진다.")
    @Test
    void createOrderItems_emptyInput_throws() {
        // given

        // when

        // then
        assertThatThrownBy(() -> orderItemService.deductStockAndCreateOrderItems(List.of(), initOrder))
                .isInstanceOf(OrderItemException.class);
        assertThatThrownBy(() -> orderItemService.deductStockAndCreateOrderItems(null, initOrder))
                .isInstanceOf(OrderItemException.class);
    }

    @DisplayName("주문에 주문상품을 추가한다.")
    @Test
    void orderItemService_creates_orderItems_and_order_adds_them() {
        // given

        when(productDetailService.deductStock(anyLong(), anyInt())).thenReturn(productDetail);

        // when
        List<OrderItem> createdItems =  orderItemService.deductStockAndCreateOrderItems(List.of(req), initOrder);
        createdItems.forEach(initOrder::addOrderItem);

        // then
        assertThat(initOrder.getOrderItems()).hasSize(1);
        OrderItem orderItem = initOrder.getOrderItems().get(0);
        assertThat(orderItem).isSameAs(createdItems.get(0));
        assertThat(orderItem.getOrder()).isSameAs(initOrder);
    }

    @DisplayName("쿠폰 적용 시 서버가 계산한 예상금액이 client 예상금액과 같으면 주문서를 확정짓는다.")
    @Test
    void finalizeOrder_withCoupon_happyCase() {
        // given
        UseCouponResponse response = UseCouponResponse.of(10000L,1000L, 9000L);
        when(couponService.verifyCoupon(eq(createOrderRequest.orderCouponId()), any(UseCouponRequest.class))).thenReturn(response);

        // when
        Order result = orderService.finalizeOrderWithCoupon(initOrderWithItems, createOrderRequest);

        // then
        assertThat(result.getTotalPrice()).isEqualTo(createOrderRequest.clientCalculatedAmount());
        assertThat(result.getCouponId()).isEqualTo(createOrderRequest.orderCouponId());
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);

        verify(couponService, times(1)).verifyCoupon(1L, new UseCouponRequest(10000L));
    }

    @DisplayName("쿠폰 적용 시 서버가 계산한 예상금액이 client 예상금액과 다르면 예외를 던진다.")
    @Test
    void finalizeOrder_withCoupon_amountMismatch_throws() {
        // given
        UseCouponResponse response = UseCouponResponse.of(10000L,0L, 10000L);
        when(couponService.verifyCoupon(eq(createOrderRequest.orderCouponId()), any(UseCouponRequest.class))).thenReturn(response);

        // when

        // then
        assertThatThrownBy(() -> orderService.finalizeOrderWithCoupon(initOrderWithItems, createOrderRequest))
                .isInstanceOf(OrderException.class);
    }

    @DisplayName("쿠폰 적용을 안해도 주문서를 확정 지을 수 있다.")
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

    @DisplayName("쿠폰 적용 안할 시 서버와 client 예상 금액이 다르면 예외를 던진다.")
    @Test
    void finalizeOrder_withoutCoupon_amountMismatch_throws() {
        // given
        CreateOrderRequest request = new CreateOrderRequest(null, null, 9000L, null);

        // when

        // then
        assertThatThrownBy(() -> orderService.finalizeOrderWithCoupon(initOrderWithItems, request))
                .isInstanceOf(OrderException.class);
    }


}
