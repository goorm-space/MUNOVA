package com.space.munova.order.service;

import com.space.munova.coupon.service.CouponService;
import com.space.munova.member.entity.Member;
import com.space.munova.order.dto.*;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.exception.OrderItemException;
import com.space.munova.order.repository.OrderItemRepository;
import com.space.munova.payment.service.PaymentService;
import com.space.munova.product.application.ProductDetailService;
import com.space.munova.product.application.exception.ProductDetailException;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.recommend.service.RecommendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private RecommendService recommendService;

    @Captor
    private ArgumentCaptor<Long> productIdCaptor;

    private Member member;
    private OrderItemRequest req;
    private Order initOrder;
    private ProductDetail productDetail;
    private OrderItem orderItem;
    private Order order;

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

        order = mock(Order.class);

        orderItem = mock(OrderItem.class);
    }

    @DisplayName("[주문서 생성] (HappyCase) 한 개의 주문 요청 상품에 대해 주문상품 엔티티(orderItem)을 생성한다.")
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

    @DisplayName("[주문서 생성] orderItem을 생성할 때 재고가 없으면 noStockException 예외를 던진다.")
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

    @DisplayName("[주문서 생성] orderItem을 생성할 때 주문 요청 상품이 없으면 noOrderItemsNotAllowedException 예외를 던진다.")
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

    @DisplayName("[주문서 생성] 주문에 주문상품을 추가한다.")
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

    @Test
    @DisplayName("[주문 취소] (HappyCase) 정상적으로 주문 취소 처리")
    void cancelOrderItem_orderCancel_HappyCase() {
        // given
        CancelOrderItemRequest request = new CancelOrderItemRequest(CancelType.ORDER_CANCEL, null, null);

        when(member.getId()).thenReturn(1L);

        when(order.getId()).thenReturn(10L);
        when(order.getMember()).thenReturn(member);

        when(orderItem.getOrder()).thenReturn(order);
        when(orderItem.getId()).thenReturn(1L);
        when(orderItem.getStatus()).thenReturn(OrderStatus.PAID);
        when(orderItem.getProductDetail()).thenReturn(productDetail);


        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));
        when(orderItemRepository.findProductDetailIdsByOrderItemIds(List.of(1L)))
                .thenReturn(List.of(100L));
        when(productDetailService.findProductIdByDetailId(100L)).thenReturn(200L);

        // doNothing()으로 외부 호출 stub 처리
        doNothing().when(paymentService).cancelPaymentAndSaveRefund(anyLong(), anyLong(), any());
        doNothing().when(recommendService).updateUserAction(anyLong(), anyInt(), any(), any(), anyBoolean());
        doNothing().when(orderItem).updateStatus(any());

        // when
        orderItemService.cancelOrderItem(1L, request, 1L);

        // then
        verify(paymentService, times(1)).cancelPaymentAndSaveRefund(1L, 10L, request);
        verify(orderItem).updateStatus(OrderStatus.CANCELED);
        verify(recommendService, times(1)).updateUserAction(200L, 0, null, null, false);
    }

    @Test
    @DisplayName("[주문 취소] 주문 항목이 존재하지 않음")
    void cancelOrderItem_orderItemNotFound() {
        // given
        CancelOrderItemRequest request = new CancelOrderItemRequest(CancelType.ORDER_CANCEL, null, null);

        when(orderItemRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderItemService.cancelOrderItem(1L, request, 1L))
                .isInstanceOf(OrderItemException.class);
    }



}
