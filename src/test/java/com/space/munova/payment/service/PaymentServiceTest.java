package com.space.munova.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.space.munova.auth.service.AuthService;
import com.space.munova.coupon.service.CouponService;
import com.space.munova.member.entity.Member;
import com.space.munova.notification.service.NotificationService;
import com.space.munova.order.dto.CancelOrderItemRequest;
import com.space.munova.order.dto.CancelType;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.service.OrderQueryServiceImpl;
import com.space.munova.payment.client.TossApiClient;
import com.space.munova.payment.dto.*;
import com.space.munova.payment.entity.*;
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

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock
    private OrderQueryServiceImpl orderQueryService;
    @Mock
    private AuthService authService;
    @Mock
    private TossApiClient tossApiClient;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RefundRepository refundRepository;
    @Mock
    private CouponService couponService;
    @Mock
    private CartService cartService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private PaymentService spyPaymentService = new PaymentServiceImpl(
            null, null, null, null, null, null, null, null, null // 의존성 주입을 위해 필요한 인자를 null로 대체
    );

    private final Long ORDER_ID = 1L;
    private final String ORDER_NUM = "ORD12345";
    private final Long MEMBER_ID = 10L;
    private final Long TOTAL_PRICE = 10000L;
    private final String PAYMENT_KEY = "toss_p_key";
    private final Long ORDER_ITEM_ID = 100L;
    private final String TRANSACTION_KEY = "transaction_key";
    private final Long PAYMENT_ID = 1L;

    // Confirm Mock 객체
    private Order mockOrder;
    private Member mockMember;
    private ConfirmPaymentRequest confirmRequest;
    private TossPaymentResponse response;

    // Cancel Mock 객체
    private Payment mockPayment;
    private CancelOrderItemRequest cancelRequest;
    private TossPaymentResponse cancelResponse;
    private CancelDto cancelDto;

    @BeforeEach
    void setUp() {
        mockMember = mock(Member.class);

        mockOrder = mock(Order.class);

        // ConfirmPaymentRequest 설정
        confirmRequest = new ConfirmPaymentRequest(PAYMENT_KEY, ORDER_NUM, TOTAL_PRICE);

        // TossPaymentResponse 성공 응답 설정
        response = mock(TossPaymentResponse.class);

        // Spy 객체에 Mock 주입 (BeforeEach마다 초기화)
        spyPaymentService = new PaymentServiceImpl(
                orderQueryService, authService, tossApiClient, paymentRepository, refundRepository,
                couponService, cartService, notificationService, eventPublisher
        );

        mockPayment = mock(Payment.class);

        cancelRequest = new CancelOrderItemRequest(
                CancelType.ORDER_CANCEL,
                CancelReason.ORDER_MISTAKE,
                TOTAL_PRICE
        );

        cancelDto = mock(CancelDto.class);

        cancelResponse = mock(TossPaymentResponse.class);
    }

    // --- 1. 결제 승인 테스트 (confirmPaymentAndSavePayment) ---
    @DisplayName("[결제 승인] (HappyCase) 결제 승인 후 모든 후처리 로직이 호출되어야 한다")
    @Test
    void confirmPayment_happyCase() {
        // given
        when(orderQueryService.getOrderByOrderNum(ORDER_NUM)).thenReturn(mockOrder);
        when(mockOrder.getMember()).thenReturn(mockMember);
        when(mockOrder.getTotalPrice()).thenReturn(TOTAL_PRICE);
        when(mockOrder.getOrderNum()).thenReturn(ORDER_NUM);
        when(mockOrder.getId()).thenReturn(ORDER_ID);
        when(mockOrder.getOrderItems()).thenReturn(List.of(mock(OrderItem.class)));
        when(mockOrder.getCouponId()).thenReturn(null);

        when(mockMember.getId()).thenReturn(MEMBER_ID);
        doNothing().when(authService).verifyAuthorization(anyLong(), anyLong());

        when(tossApiClient.sendConfirmRequest(any(ConfirmPaymentRequest.class))).thenReturn(response);

        when(response.paymentKey()).thenReturn(PAYMENT_KEY);
        when(response.status()).thenReturn(PaymentStatus.DONE);
        when(response.totalAmount()).thenReturn(TOTAL_PRICE);
        when(response.receipt()).thenReturn(new ReceiptInfo("http://url.com"));
        when(response.requestedAt()).thenReturn(ZonedDateTime.now());

        Payment mockPayment = mock(Payment.class);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // when
        paymentService.confirmPaymentAndSavePayment(confirmRequest, MEMBER_ID);

        // then
        // 1. 주문 금액 검증은 validateAmount 내부에서 처리되므로 Toss API 통신이 실행되어야 함
        verify(tossApiClient, times(1)).sendConfirmRequest(any(ConfirmPaymentRequest.class));

        // 2. 주문 상태 변경
        verify(mockOrder, times(1)).updateStatus(OrderStatus.PAID);

        // 3. Payment 저장
        verify(paymentRepository, times(1)).save(any(Payment.class));

        // 4. 후처리 로직 호출
        verify(eventPublisher, times(1)).publishEvent(any(PaymentCompensationEvent.class));
        verify(cartService, times(1)).deleteByOrderItemsAndMemberId(any(), eq(MEMBER_ID));

        // 5. 쿠폰 (null이므로 호출되지 않아야 함)
        verify(couponService, never()).useCoupon(anyLong());
    }

    @DisplayName("[결제 승인] 요청 금액이 주문 금액과 일치하지 않을 경우 예외가 발생해야 한다")
    @Test
    void confirmPayment_RequestAmountMismatch_ThrowsException() {
        // given
        ConfirmPaymentRequest mismatchRequest = new ConfirmPaymentRequest(PAYMENT_KEY, ORDER_NUM, 49999L); // 금액 불일치
        when(orderQueryService.getOrderByOrderNum(ORDER_NUM)).thenReturn(mockOrder);
        when(mockOrder.getMember()).thenReturn(mockMember);
        when(mockOrder.getTotalPrice()).thenReturn(TOTAL_PRICE);
        when(mockMember.getId()).thenReturn(MEMBER_ID);

        // WHEN & THEN
        assertThatThrownBy(() -> paymentService.confirmPaymentAndSavePayment(mismatchRequest, MEMBER_ID))
                .isInstanceOf(PaymentException.class);

        // Toss API 호출 및 후처리 로직은 실행되지 않아야 함
        verify(tossApiClient, never()).sendConfirmRequest(any());
        verify(paymentRepository, never()).save(any());
    }

    @DisplayName("[결제 승인] Toss 응답의 상태가 DONE이 아닐 경우 예외가 발생해야 한다")
    @Test
    void confirmPayment_responseStatusNotDone_throws() throws JsonProcessingException {
        // given
        TossPaymentResponse failedResponse = mock(TossPaymentResponse.class);
        when(failedResponse.status()).thenReturn(PaymentStatus.ABORTED);

        when(orderQueryService.getOrderByOrderNum(ORDER_NUM)).thenReturn(mockOrder);
        when(mockOrder.getMember()).thenReturn(mockMember);
        when(mockOrder.getTotalPrice()).thenReturn(TOTAL_PRICE);
        when(mockMember.getId()).thenReturn(MEMBER_ID);

        when(tossApiClient.sendConfirmRequest(any())).thenReturn(failedResponse);

        // when / then
        assertThatThrownBy(() -> paymentService.confirmPaymentAndSavePayment(confirmRequest, MEMBER_ID))
                .isInstanceOf(PaymentException.class);

        verify(paymentRepository, never()).save(any());
        verify(mockOrder, never()).updateStatus(any());
    }

    @DisplayName("[결제 승인] (HappyCase) 쿠폰이 있을 경우 couponService.useCoupon 호출")
    @Test
    void confirmPayment_whenCouponExists() {
        // given
        when(orderQueryService.getOrderByOrderNum(ORDER_NUM)).thenReturn(mockOrder);
        when(mockOrder.getMember()).thenReturn(mockMember);
        when(mockOrder.getTotalPrice()).thenReturn(TOTAL_PRICE);
        when(mockOrder.getOrderNum()).thenReturn(ORDER_NUM);
        when(mockOrder.getId()).thenReturn(ORDER_ID);
        when(mockOrder.getOrderItems()).thenReturn(List.of(mock(OrderItem.class)));
        when(mockOrder.getCouponId()).thenReturn(1L);

        when(mockMember.getId()).thenReturn(MEMBER_ID);
        doNothing().when(authService).verifyAuthorization(anyLong(), anyLong());

        when(tossApiClient.sendConfirmRequest(any(ConfirmPaymentRequest.class))).thenReturn(response);

        when(response.paymentKey()).thenReturn(PAYMENT_KEY);
        when(response.status()).thenReturn(PaymentStatus.DONE);
        when(response.totalAmount()).thenReturn(TOTAL_PRICE);
        when(response.receipt()).thenReturn(new ReceiptInfo("http://url.com"));
        when(response.requestedAt()).thenReturn(ZonedDateTime.now());

        Payment mockPayment = mock(Payment.class);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // when
        paymentService.confirmPaymentAndSavePayment(confirmRequest, MEMBER_ID);

        // then
        verify(couponService, times(1)).useCoupon(anyLong());
    }

    @DisplayName("[환불 승인] (HappyCase) 결제 취소/환불 정보를 저장한다.")
    @Test
    void cancelPaymentAndSaveRefund_happyCase() {
        // GIVEN
        when(paymentRepository.findPaymentByOrderId(ORDER_ID)).thenReturn(Optional.of(mockPayment));
        when(mockPayment.getTossPaymentKey()).thenReturn(PAYMENT_KEY);

        when(tossApiClient.sendCancelRequest(eq(PAYMENT_KEY), any(CancelPaymentRequest.class))).thenReturn(cancelResponse);

        when(cancelResponse.cancels()).thenReturn(List.of(cancelDto));
        when(cancelResponse.status()).thenReturn(PaymentStatus.CANCELED);
        when(cancelResponse.lastTransactionKey()).thenReturn(TRANSACTION_KEY);

        when(cancelDto.transactionKey()).thenReturn(TRANSACTION_KEY);
        when(cancelDto.cancelStatus()).thenReturn(CancelStatus.DONE);
        when(cancelDto.canceledAt()).thenReturn(ZonedDateTime.now());

        when(refundRepository.findByTransactionKey(anyString())).thenReturn(Optional.empty());

        when(mockPayment.getId()).thenReturn(PAYMENT_ID);

        // WHEN
        paymentService.cancelPaymentAndSaveRefund(ORDER_ITEM_ID, ORDER_ID, cancelRequest);

        // THEN
        // 1. API 호출
        verify(tossApiClient, times(1)).sendCancelRequest(eq(PAYMENT_KEY), any(CancelPaymentRequest.class));

        // 2. 상태 업데이트
        verify(mockPayment, times(1)).updatePaymentInfo(eq(PaymentStatus.CANCELED), eq(TRANSACTION_KEY));

        // 3. Refund 저장
        verify(refundRepository, times(1)).save(any(Refund.class));
    }

    @DisplayName("[환불 승인] 이미 환불 정보가 존재하면 save 호출 안 함")
    @Test
    void cancelPaymentAndSaveRefund_refundAlreadyExists() throws JsonProcessingException {
        // given
        when(paymentRepository.findPaymentByOrderId(ORDER_ID)).thenReturn(Optional.of(mockPayment));
        when(mockPayment.getTossPaymentKey()).thenReturn(PAYMENT_KEY);

        when(tossApiClient.sendCancelRequest(eq(PAYMENT_KEY), any(CancelPaymentRequest.class))).thenReturn(cancelResponse);

        when(cancelResponse.cancels()).thenReturn(List.of(cancelDto));

        when(cancelDto.transactionKey()).thenReturn(TRANSACTION_KEY);
        when(cancelDto.cancelStatus()).thenReturn(CancelStatus.DONE);

        when(refundRepository.findByTransactionKey(anyString())).thenReturn(Optional.of(mock(Refund.class)));


        // when
        paymentService.cancelPaymentAndSaveRefund(ORDER_ITEM_ID, ORDER_ID, cancelRequest);

        // then
        // 1. API 호출은 실행되어야 함
        verify(tossApiClient, times(1)).sendCancelRequest(eq(PAYMENT_KEY), any(CancelPaymentRequest.class));

        // 2. 아래 로직은 호출되지 않아야 함
        verify(refundRepository, never()).save(any(Refund.class));
        verify(mockPayment, never()).updatePaymentInfo(any(PaymentStatus.class), anyString());
    }

    @Test
    @DisplayName("[환불 승인] 환불 상태가 DONE이 아닐 경우 예외가 발생해야 한다")
    void cancelPayment_StatusNotAccepted_ThrowsException() {
        // GIVEN
        when(paymentRepository.findPaymentByOrderId(ORDER_ID)).thenReturn(Optional.of(mockPayment));
        when(mockPayment.getTossPaymentKey()).thenReturn(PAYMENT_KEY);

        when(tossApiClient.sendCancelRequest(eq(PAYMENT_KEY), any(CancelPaymentRequest.class))).thenReturn(cancelResponse);

        when(cancelResponse.cancels()).thenReturn(List.of(cancelDto));

        CancelStatus mockCancelStatus = mock(CancelStatus.class);
        when(cancelDto.transactionKey()).thenReturn(TRANSACTION_KEY);
        when(cancelDto.cancelStatus()).thenReturn(mockCancelStatus);
        when(mockCancelStatus.isDone()).thenReturn(false);
//        when(cancelDto.canceledAt()).thenReturn(ZonedDateTime.now());

        // WHEN & THEN
        assertThatThrownBy(() -> paymentService.cancelPaymentAndSaveRefund(ORDER_ITEM_ID, ORDER_ID, cancelRequest))
                .isInstanceOf(PaymentException.class);

        // Refund 저장은 실행되지 않아야 함
        verify(refundRepository, never()).save(any());
    }

}
