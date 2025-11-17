package com.space.munova.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.munova.coupon.service.CouponService;
import com.space.munova.notification.dto.NotificationPayload;
import com.space.munova.notification.service.NotificationService;
import com.space.munova.order.dto.CancelOrderItemRequest;
import com.space.munova.order.dto.CancelType;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.exception.OrderException;
import com.space.munova.order.repository.OrderRepository;
import com.space.munova.payment.client.TossApiClient;
import com.space.munova.payment.dto.*;
import com.space.munova.payment.entity.Payment;
import com.space.munova.payment.entity.PaymentMethod;
import com.space.munova.payment.entity.PaymentStatus;
import com.space.munova.payment.entity.Refund;
import com.space.munova.payment.event.PaymentCompensationEvent;
import com.space.munova.payment.exception.PaymentException;
import com.space.munova.payment.repository.PaymentRepository;
import com.space.munova.payment.repository.RefundRepository;
import com.space.munova.product.application.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private TossApiClient tossApiClient;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private CartService cartService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private CouponService couponService;
    @Mock
    private RefundRepository refundRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;
    @Captor
    private ArgumentCaptor<PaymentCompensationEvent> eventCaptor;
    @Captor
    private ArgumentCaptor<Refund> refundCaptor;

    private ConfirmPaymentRequest confirmPaymentRequest;
    private Order order;
    private String tossRawJson;
    private CancelOrderItemRequest cancelOrderItemRequest;
    private Payment payment;

    @BeforeEach
    void setUp() {
        confirmPaymentRequest = new ConfirmPaymentRequest("paymentKey", "orderId", 10000L);

        order = spy(Order.builder()
                .id(1L)
                .orderNum(confirmPaymentRequest.orderId())
                .totalPrice(10000L)
                .build()
        );

        tossRawJson = "{\"some\":\"json\"}";

        cancelOrderItemRequest = new CancelOrderItemRequest(CancelType.ORDER_CANCEL, CancelReason.ORDER_MISTAKE, 10000L);

        payment = mock(Payment.class);
    }

    @DisplayName("[결제 승인] (HappyCase) TossPayment 결제 승인")
    @Test
    void confirmPayment_happyCase() throws IOException {
        // given
        when(tossApiClient.sendConfirmRequest(confirmPaymentRequest)).thenReturn(tossRawJson);

        ReceiptInfo receipt = new ReceiptInfo("http://receipt.com");
        TossPaymentResponse tossResponse = mock(TossPaymentResponse.class);
        when(tossResponse.paymentKey()).thenReturn(confirmPaymentRequest.paymentKey());
        when(tossResponse.status()).thenReturn(PaymentStatus.DONE);
        when(tossResponse.method()).thenReturn(PaymentMethod.카드);
        when(tossResponse.totalAmount()).thenReturn(confirmPaymentRequest.amount());
        when(tossResponse.requestedAt()).thenReturn(ZonedDateTime.now().minusMinutes(5));
        when(tossResponse.approvedAt()).thenReturn(ZonedDateTime.now());
        when(tossResponse.receipt()).thenReturn(receipt);
        when(tossResponse.lastTransactionKey()).thenReturn("lastTransactionKey");

        when(objectMapper.readValue(tossRawJson, TossPaymentResponse.class)).thenReturn(tossResponse);

        when(orderRepository.findByOrderNum(confirmPaymentRequest.orderId())).thenReturn(Optional.of(order));

        when(order.getCouponId()).thenReturn(null);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        doNothing().when(cartService).deleteByProductDetailIdsAndMemberId(anyList(), anyLong());

        doNothing().when(notificationService).sendNotification(any(NotificationPayload.class));

        // when
        paymentService.confirmPaymentAndSavePayment(confirmPaymentRequest, 1L);

        // then
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        PaymentCompensationEvent published = eventCaptor.getValue();
        assertThat(published.paymentKey()).isEqualTo(confirmPaymentRequest.paymentKey());
        assertThat(published.orderNum()).isEqualTo(confirmPaymentRequest.orderId());
        assertThat(published.amount()).isEqualTo(confirmPaymentRequest.amount());

        verify(orderRepository, times(1)).findByOrderNum(confirmPaymentRequest.orderId());
        verify(order).updateStatus(OrderStatus.PAID);

        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(order.getId());
        assertThat(saved.getTossPaymentKey()).isEqualTo(tossResponse.paymentKey());
        assertThat(saved.getStatus()).isEqualTo(tossResponse.status());
        assertThat(saved.getTotalAmount()).isEqualTo(tossResponse.totalAmount());

        verify(cartService, times(1)).deleteByProductDetailIdsAndMemberId(anyList(), anyLong());
        verify(notificationService, times(1)).sendNotification(any(NotificationPayload.class));
    }

    @DisplayName("[결제 승인] orderNum에 해당하는 주문을 찾지 못하면 OrderException 발생")
    @Test
    void confirmPayment_orderNotFound_throws() {
        // given
        when(orderRepository.findByOrderNum(confirmPaymentRequest.orderId())).thenReturn(Optional.empty());

        // when / then
        assertThrows(OrderException.class, () -> paymentService.confirmPaymentAndSavePayment(confirmPaymentRequest, 1L));

        // event나 다른 작업이 발생하지 않아야 함
        verify(eventPublisher, never()).publishEvent(any(PaymentCompensationEvent.class));
        verifyNoInteractions(couponService);
        verify(paymentRepository, never()).save(any());
    }

    @DisplayName("[결제 승인] 요청 금액과 주문 총액 불일치")
    @Test
    void confirmPayment_requestAmountMismatch_throws() {
        // given
        when(orderRepository.findByOrderNum(confirmPaymentRequest.orderId())).thenReturn(Optional.of(order));
        when(order.getTotalPrice()).thenReturn(9999L); // 요청 금액(1000)과 다름

        // when / then
        assertThatThrownBy(() -> paymentService.confirmPaymentAndSavePayment(confirmPaymentRequest, 1L))
                .isInstanceOf(PaymentException.class);

        verify(eventPublisher, never()).publishEvent(any(PaymentCompensationEvent.class));
        verifyNoInteractions(objectMapper);
    }

    @DisplayName("[결제 승인] Toss 응답 상태가 DONE이 아니면 PaymentException 발생")
    @Test
    void confirmPayment_responseStatusNotDone_throws() throws JsonProcessingException {
        // given
        when(tossApiClient.sendConfirmRequest(confirmPaymentRequest)).thenReturn(tossRawJson);

        TossPaymentResponse tossResponse = mock(TossPaymentResponse.class);
        when(tossResponse.status()).thenReturn(PaymentStatus.CANCELED); // DONE 아님
        when(objectMapper.readValue(tossRawJson, TossPaymentResponse.class)).thenReturn(tossResponse);
        when(orderRepository.findByOrderNum(confirmPaymentRequest.orderId())).thenReturn(Optional.of(order));
        when(order.getTotalPrice()).thenReturn(confirmPaymentRequest.amount());

        // when / then
        assertThatThrownBy(() -> paymentService.confirmPaymentAndSavePayment(confirmPaymentRequest, 1L))
                .isInstanceOf(PaymentException.class);

        verify(eventPublisher, times(1)).publishEvent(any(PaymentCompensationEvent.class));
        verifyNoInteractions(couponService);
        verify(paymentRepository, never()).save(any());
    }

    @DisplayName("[결제 승인] Toss응답 금액과 주문 금액 불일치")
    @Test
    void confirmPaymentAndSavePayment_amountMismatch_throwsPaymentException() throws Exception {
        // given
        when(tossApiClient.sendConfirmRequest(confirmPaymentRequest)).thenReturn(tossRawJson);

        TossPaymentResponse tossResponse = mock(TossPaymentResponse.class);
        when(tossResponse.status()).thenReturn(PaymentStatus.DONE);
        when(tossResponse.totalAmount()).thenReturn(confirmPaymentRequest.amount() - 1000L);
        when(objectMapper.readValue(tossRawJson, TossPaymentResponse.class)).thenReturn(tossResponse);

        when(orderRepository.findByOrderNum(confirmPaymentRequest.orderId())).thenReturn(Optional.of(order));

        // when

        // then
        assertThatThrownBy(() -> paymentService.confirmPaymentAndSavePayment(confirmPaymentRequest, 1L))
                .isInstanceOf(PaymentException.class);
        verify(eventPublisher, times(1)).publishEvent(any(PaymentCompensationEvent.class));
        verify(paymentRepository, never()).save(any());
        verify(cartService, never()).deleteByProductDetailIdsAndMemberId(anyList(), anyLong());
        verify(notificationService, never()).sendNotification(any());
    }

    @DisplayName("[결제 승인] 쿠폰이 있을 경우 couponService.useCoupon 호출")
    @Test
    void confirmPayment_whenCouponExists() throws JsonProcessingException {
        // given
        when(tossApiClient.sendConfirmRequest(confirmPaymentRequest)).thenReturn(tossRawJson);

        ReceiptInfo receipt = new ReceiptInfo("http://receipt.com");
        TossPaymentResponse tossResponse = mock(TossPaymentResponse.class);
        when(tossResponse.paymentKey()).thenReturn(confirmPaymentRequest.paymentKey());
        when(tossResponse.status()).thenReturn(PaymentStatus.DONE);
        when(tossResponse.method()).thenReturn(PaymentMethod.카드);
        when(tossResponse.totalAmount()).thenReturn(confirmPaymentRequest.amount());
        when(tossResponse.requestedAt()).thenReturn(ZonedDateTime.now().minusMinutes(5));
        when(tossResponse.approvedAt()).thenReturn(ZonedDateTime.now());
        when(tossResponse.receipt()).thenReturn(receipt);
        when(tossResponse.lastTransactionKey()).thenReturn("lastTransactionKey");

        when(objectMapper.readValue(tossRawJson, TossPaymentResponse.class)).thenReturn(tossResponse);

        Order order = mock(Order.class);
        when(order.getId()).thenReturn(42L);
        when(order.getTotalPrice()).thenReturn(confirmPaymentRequest.amount());
        when(order.getOrderNum()).thenReturn(confirmPaymentRequest.orderId());
        when(order.getCouponId()).thenReturn(123L); // 쿠폰 있음
        when(order.getOrderItems()).thenReturn(List.of());
        when(orderRepository.findByOrderNum(confirmPaymentRequest.orderId())).thenReturn(Optional.of(order));

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(couponService).useCoupon(123L);
        doNothing().when(cartService).deleteByProductDetailIdsAndMemberId(anyList(), anyLong());
        doNothing().when(notificationService).sendNotification(any());

        // when
        paymentService.confirmPaymentAndSavePayment(confirmPaymentRequest, 1L);

        // then
        verify(couponService, times(1)).useCoupon(123L);
    }

    @DisplayName("[환불 승인] (HappyCase) 환불 처리 및 환불 정보를 저장한다.")
    @Test
    void cancelPaymentAndSaveRefund_happyCase() throws JsonProcessingException {
        // given
        Long orderId = 1L;
        Long orderItemId = 2L;

        Payment realPayment = new Payment(); // 실제 객체 생성
        Payment spyPayment = spy(realPayment);

        when(paymentRepository.findPaymentByOrderId(orderId)).thenReturn(Optional.of(spyPayment));

        when(tossApiClient.sendCancelRequest(any(), any(CancelPaymentRequest.class)))
                .thenReturn(tossRawJson);

        TossPaymentResponse tossResponse = mock(TossPaymentResponse.class);
        CancelDto cancelDto = mock(CancelDto.class);

        when(cancelDto.transactionKey()).thenReturn("transactionKey");
        when(cancelDto.cancelReason()).thenReturn(cancelOrderItemRequest.cancelReason().toString());
        when(cancelDto.cancelAmount()).thenReturn(cancelOrderItemRequest.cancelAmount());
        when(cancelDto.cancelStatus()).thenReturn("DONE");
        when(cancelDto.canceledAt()).thenReturn(ZonedDateTime.now());

        when(tossResponse.cancels()).thenReturn(List.of(cancelDto));
        when(tossResponse.paymentKey()).thenReturn("paymentKey");

        when(objectMapper.readValue(tossRawJson, TossPaymentResponse.class)).thenReturn(tossResponse);

        when(refundRepository.findByTransactionKey(anyString())).thenReturn(Optional.empty());
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));

        doNothing().when(spyPayment).updatePaymentInfo(eq(tossResponse), anyString());

        // when
        paymentService.cancelPaymentAndSaveRefund(orderItemId, orderId, cancelOrderItemRequest);

        // then
        verify(spyPayment, times(1)).updatePaymentInfo(eq(tossResponse), anyString());

        verify(refundRepository, times(1)).save(refundCaptor.capture());
        Refund saved = refundCaptor.getValue();
        assertThat(saved.getOrderItemId()).isEqualTo(orderItemId);
        assertThat(saved.getPaymentKey()).isEqualTo(tossResponse.paymentKey());
        assertThat(saved.getTransactionKey()).isEqualTo(cancelDto.transactionKey());
        assertThat(saved.getCancelAmount()).isEqualTo(cancelOrderItemRequest.cancelAmount());
    }

    @DisplayName("[환불 승인] 이미 환불 정보가 존재하면 save 호출 안 함")
    @Test
    void cancelPaymentAndSaveRefund_refundAlreadyExists() throws JsonProcessingException {
        // given
        Long orderId = 1L;
        Long orderItemId = 2L;

        Payment realPayment = new Payment();
        Payment spyPayment = spy(realPayment);

        when(paymentRepository.findPaymentByOrderId(orderId)).thenReturn(Optional.of(spyPayment));
        when(tossApiClient.sendCancelRequest(any(), any(CancelPaymentRequest.class)))
                .thenReturn(tossRawJson);

        TossPaymentResponse tossResponse = mock(TossPaymentResponse.class);
        CancelDto cancelDto = mock(CancelDto.class);

        when(cancelDto.transactionKey()).thenReturn("transactionKey");

        when(tossResponse.cancels()).thenReturn(List.of(cancelDto));
        when(objectMapper.readValue(anyString(), eq(TossPaymentResponse.class))).thenReturn(tossResponse);

        when(refundRepository.findByTransactionKey("transactionKey")).thenReturn(Optional.of(mock(Refund.class)));

        // when
        paymentService.cancelPaymentAndSaveRefund(orderItemId, orderId, cancelOrderItemRequest);

        // then
        verify(spyPayment, never()).updatePaymentInfo(any(), anyString());
        verify(refundRepository, never()).save(any());
    }


}
