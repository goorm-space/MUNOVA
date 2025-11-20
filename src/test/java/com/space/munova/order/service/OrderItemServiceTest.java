package com.space.munova.order.service;

import com.space.munova.auth.exception.AuthException;
import com.space.munova.auth.service.AuthService;
import com.space.munova.member.entity.Member;
import com.space.munova.order.dto.*;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.exception.OrderItemException;
import com.space.munova.order.repository.OrderItemRepository;
import com.space.munova.order.service.cancel.OrderCancelStrategy;
import com.space.munova.order.service.cancel.ReturnRefundStrategy;
import com.space.munova.payment.dto.CancelReason;
import com.space.munova.payment.service.PaymentService;
import com.space.munova.product.application.ProductDetailService;
import com.space.munova.product.application.exception.ProductDetailException;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.recommend.service.RecommendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
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
    @Mock
    private AuthService authService;

    @Mock
    private OrderCancelStrategy orderCancelStrategy;
    @Mock
    private ReturnRefundStrategy returnRefundStrategy;


    private final Long DETAIL_ID_1 = 10L;
    private final int QUANTITY_1 = 2;
    private final Long DETAIL_ID_2 = 20L;
    private final int QUANTITY_2 = 5;
    private final Long TEST_ORDER_ITEM_ID = 100L;
    private final Long OWNER_MEMBER_ID = 1L;
    private final Long OTHER_MEMBER_ID = 2L;
    private final Long PRODUCT_DETAIL_ID = 5L;
    private final Long PRODUCT_ID = 1L;
    private final int QUANTITY = 2;
    private final Long ORDER_ID = 50L;
    private final Long CANCEL_AMOUNT = 10000L;

    private Order mockOrder;
    private ProductDetail mockProductDetail;
    private OrderItemRequest request1;
    private OrderItemRequest request2;
    private List<OrderItemRequest> itemRequests;
    private OrderItem mockOrderItem;
    private CancelOrderItemRequest cancelRequest;

    @BeforeEach
    void setUp() {
        mockOrder = mock(Order.class);
        mockProductDetail = mock(ProductDetail.class);
        mockOrderItem = mock(OrderItem.class);

        request1 = new OrderItemRequest(DETAIL_ID_1, QUANTITY_1);
        request2 = new OrderItemRequest(DETAIL_ID_2, QUANTITY_2);
        itemRequests = List.of(request1, request2);
    }

    // --- 1. Happy Case: OrderItem 생성 및 재고 차감 성공 ---

    @Test
    @DisplayName("재고 차감 및 OrderItem 생성에 성공해야 하며, 요청 수만큼 OrderItem이 반환되어야 한다")
    void deductStockAndCreateOrderItems_Success() {
        // GIVEN
        // 1. OrderItem.create의 정적 메서드 Mocking 설정
        OrderItem mockOrderItem1 = mock(OrderItem.class);
        OrderItem mockOrderItem2 = mock(OrderItem.class);

        when(productDetailService.deductStock(anyLong(), anyInt())).thenReturn(mockProductDetail);

        try (MockedStatic<OrderItem> orderItemMockedStatic = Mockito.mockStatic(OrderItem.class)) {

            // 2. OrderItem.create가 호출될 때 Mock 객체 반환하도록 Stubbing
            // 첫 번째 호출
            orderItemMockedStatic.when(() -> OrderItem.create(eq(mockOrder), eq(mockProductDetail), eq(QUANTITY_1)))
                    .thenReturn(mockOrderItem1);
            // 두 번째 호출
            orderItemMockedStatic.when(() -> OrderItem.create(eq(mockOrder), eq(mockProductDetail), eq(QUANTITY_2)))
                    .thenReturn(mockOrderItem2);

            // WHEN
            List<OrderItem> createdItems = orderItemService.deductStockAndCreateOrderItems(itemRequests, mockOrder);

            // THEN
            // 1. 반환된 리스트 검증
            assertThat(createdItems).hasSize(2);
            assertThat(createdItems).containsExactly(mockOrderItem1, mockOrderItem2);

            // 2. ProductDetailService 호출 검증 (재고 차감 책임 확인)
            verify(productDetailService, times(1)).deductStock(DETAIL_ID_1, QUANTITY_1);
            verify(productDetailService, times(1)).deductStock(DETAIL_ID_2, QUANTITY_2);

            // 3. OrderItem.create 호출 검증 (OrderItem 생성 책임 확인)
            orderItemMockedStatic.verify(() -> OrderItem.create(any(), any(), anyInt()), times(2));
        }
    }

    // --- 2. 실패 케이스: 재고 차감 실패 (Service 예외 전파) ---
    @Test
    @DisplayName("ProductDetailService에서 예외 발생 시, OrderItem 생성이 중단되고 예외를 전파해야 한다")
    void deductStockAndCreateOrderItems_DeductStockFails_ShouldThrowException() {
        // GIVEN
        // ProductDetailService의 첫 번째 호출에서 RuntimeException을 던지도록 Mock 설정 (재고 부족 등)
        doThrow(ProductDetailException.stockInsufficientException()).when(productDetailService)
                .deductStock(DETAIL_ID_1, QUANTITY_1);

        // WHEN & THEN
        assertThatThrownBy(() -> orderItemService.deductStockAndCreateOrderItems(itemRequests, mockOrder))
                .isInstanceOf(ProductDetailException.class);

        // 1. 두 번째 재고 차감은 호출되지 않아야 함 (루프 중단)
        verify(productDetailService, never()).deductStock(DETAIL_ID_2, QUANTITY_2);

        // 2. OrderItem 생성은 호출되지 않아야 함
        try (MockedStatic<OrderItem> orderItemMockedStatic = mockStatic(OrderItem.class)) {
            orderItemService.deductStockAndCreateOrderItems(itemRequests, mockOrder);
            orderItemMockedStatic.verify(() -> OrderItem.create(any(), any(), anyInt()), never());
        } catch (RuntimeException ignored) {
            // 예외가 발생했으므로 무시
        }
    }

    // --- 1. Happy Case: ORDER_CANCEL 성공 ---
    @Test
    @DisplayName("[주문취소] (HappyCase) ORDER_CANCEL 요청 시, 모든 메서드가 올바르게 호출되고 상태가 변경되어야 한다")
    void cancelOrderItem_OrderCancel_Success() {
        // GIVEN
        // 1. ProductDetail Mock
        when(mockProductDetail.getId()).thenReturn(PRODUCT_DETAIL_ID);

        // 2. Order Mock
        when(mockOrder.getMember()).thenReturn(mock(Member.class));
        when(mockOrder.getMember().getId()).thenReturn(OWNER_MEMBER_ID);
        when(mockOrder.getId()).thenReturn(ORDER_ID);

        // 3. OrderItem Mock
        when(mockOrderItem.getOrder()).thenReturn(mockOrder);
        when(mockOrderItem.getProductDetail()).thenReturn(mockProductDetail);
        when(mockOrderItem.getId()).thenReturn(TEST_ORDER_ITEM_ID);
        when(mockOrderItem.getQuantity()).thenReturn(QUANTITY);

        // 4. Repository 공통 Mock (조회 성공)
        when(orderItemRepository.findById(TEST_ORDER_ITEM_ID)).thenReturn(Optional.of(mockOrderItem));

        // 5. 추천 시스템 관련 Mock
        when(orderItemRepository.findProductDetailIdsByOrderItemIds(List.of(TEST_ORDER_ITEM_ID)))
                .thenReturn(List.of(PRODUCT_DETAIL_ID));
        when(productDetailService.findProductIdByDetailId(PRODUCT_DETAIL_ID))
                .thenReturn(PRODUCT_ID);

        cancelRequest = new CancelOrderItemRequest(CancelType.ORDER_CANCEL, CancelReason.CUSTOMER_SIMPLE_CHANGE, CANCEL_AMOUNT);

        // OrderItem의 현재 상태를 PAID로 설정 (유효성 검증 통과를 위해)
        when(mockOrderItem.getStatus()).thenReturn(OrderStatus.PAID);

        // WHEN
        orderItemService.cancelOrderItem(TEST_ORDER_ITEM_ID, cancelRequest, OWNER_MEMBER_ID);

        // THEN
        // 1. 권한 검증 (AuthService)
        verify(authService, times(1)).verifyAuthorization(OWNER_MEMBER_ID, OWNER_MEMBER_ID);

        // 2. 전략 패턴 호출
        verify(orderCancelStrategy, times(1)).validate(OrderStatus.PAID); // 유효성 검증
        verify(orderCancelStrategy, times(1)).updateOrderItemStatus(mockOrderItem); // 상태 변경
        verify(returnRefundStrategy, never()).validate(any()); // 다른 전략은 호출되면 안 됨

        // 3. 결제 처리 (PaymentService)
        verify(paymentService, times(1)).cancelPaymentAndSaveRefund(
                TEST_ORDER_ITEM_ID, ORDER_ID, cancelRequest);

        // 4. 재고 복구 (ProductDetailService)
        verify(productDetailService, times(1)).increaseStock(
                PRODUCT_DETAIL_ID, QUANTITY);

        // 5. 추천 시스템 업데이트 (RecommendService)
        verify(recommendService, times(1)).updateUserAction(PRODUCT_ID, 0, null, null, false);
    }

    @Test
    @DisplayName("[주문취소] 주문에 해당하는 orderItem이 없으면 예외가 발생된다.")
    void cancelOrderItem_NoOrderItem_Exception() {
        // GIVEN
        when(orderItemRepository.findById(TEST_ORDER_ITEM_ID)).thenReturn(Optional.empty());

        // WHEN&THEN
        assertThatThrownBy(() -> orderItemService.cancelOrderItem(TEST_ORDER_ITEM_ID, cancelRequest, OTHER_MEMBER_ID))
                .isInstanceOf(OrderItemException.class);
    }

    // --- 2. 실패 Case: 권한 없음 ---
    @Test
    @DisplayName("[주문취소] 다른 회원의 orderItem 취소 시도 시, AuthException이 발생해야 한다")
    void cancelOrderItem_UnauthorizedAccess_ThrowsAuthException() {
        // GIVEN
        when(mockOrder.getMember()).thenReturn(mock(Member.class));
        when(mockOrder.getMember().getId()).thenReturn(OWNER_MEMBER_ID);

        when(mockOrderItem.getOrder()).thenReturn(mockOrder);

        when(orderItemRepository.findById(TEST_ORDER_ITEM_ID)).thenReturn(Optional.of(mockOrderItem));

        cancelRequest = new CancelOrderItemRequest(CancelType.ORDER_CANCEL, CancelReason.ETC_ORDER_CANCEL, CANCEL_AMOUNT);

        // AuthService가 권한 실패 시 예외를 던지도록 Mock 설정
        doThrow(AuthException.unauthorizedException())
                .when(authService).verifyAuthorization(OWNER_MEMBER_ID, OTHER_MEMBER_ID);

        // WHEN & THEN
        assertThatThrownBy(() -> orderItemService.cancelOrderItem(TEST_ORDER_ITEM_ID, cancelRequest, OTHER_MEMBER_ID))
                .isInstanceOf(AuthException.class);

        // 핵심: 이후 로직(결제, 재고, 상태 변경)은 호출되지 않아야 함
        verify(orderCancelStrategy, never()).validate(any());
        verify(paymentService, never()).cancelPaymentAndSaveRefund(anyLong(), anyLong(), any());
        verify(productDetailService, never()).increaseStock(anyLong(), anyInt());
    }

    // --- 3. 실패 Case: 전략 유효성 검증 실패 ---

    @Test
    @DisplayName("[주문 취소] 취소 불가능한 상태일 때, OrderItemException이 발생해야 한다")
    void cancelOrderItem_ValidationFails_ThrowsOrderItemException() {
        // GIVEN
        when(mockOrder.getMember()).thenReturn(mock(Member.class));
        when(mockOrder.getMember().getId()).thenReturn(OWNER_MEMBER_ID);

        when(mockOrderItem.getOrder()).thenReturn(mockOrder);

        when(orderItemRepository.findById(TEST_ORDER_ITEM_ID)).thenReturn(Optional.of(mockOrderItem));

        cancelRequest = new CancelOrderItemRequest(CancelType.RETURN_REFUND, CancelReason.ETC_ORDER_CANCEL, CANCEL_AMOUNT);

        // OrderItem의 현재 상태를 불가능한 상태(예: SHIPPED)로 설정
        when(mockOrderItem.getStatus()).thenReturn(OrderStatus.SHIPPING);

        // 전략의 validate 메서드가 예외를 던지도록 Mock 설정
        doThrow(OrderItemException.cancellationNotAllowedException())
                .when(returnRefundStrategy).validate(OrderStatus.SHIPPING);

        // WHEN & THEN
        assertThatThrownBy(() -> orderItemService.cancelOrderItem(TEST_ORDER_ITEM_ID, cancelRequest, OWNER_MEMBER_ID))
                .isInstanceOf(OrderItemException.class);

        // 핵심: 결제, 재고, 상태 변경은 호출되지 않아야 함
        verify(paymentService, never()).cancelPaymentAndSaveRefund(anyLong(), anyLong(), any());
        verify(productDetailService, never()).increaseStock(anyLong(), anyInt());
        verify(returnRefundStrategy, never()).updateOrderItemStatus(any());
    }
}
