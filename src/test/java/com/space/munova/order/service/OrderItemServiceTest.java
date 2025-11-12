package com.space.munova.order.service;

import com.space.munova.coupon.service.CouponService;
import com.space.munova.member.entity.Member;
import com.space.munova.order.dto.CreateOrderRequest;
import com.space.munova.order.dto.OrderItemRequest;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@DisplayName("OrderItem_Service_Test")
@ExtendWith(MockitoExtension.class)
public class OrderItemServiceTest {

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    @Mock
    private ProductDetailService productDetailService;

    private Member member;
    private OrderItemRequest req;
    private Order initOrder;
    private ProductDetail productDetail;

    @BeforeEach
    void setUp() {
        member = mock(Member.class);
        req = new OrderItemRequest(1L, 2);
        initOrder = Order.createInitOrder(member, null);

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
}
