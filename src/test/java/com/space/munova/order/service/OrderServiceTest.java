package com.space.munova.order.service;

import com.space.munova.auth.exception.AuthException;
import com.space.munova.auth.service.AuthService;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.member.entity.Member;
import com.space.munova.member.service.MemberService;
import com.space.munova.order.dto.*;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.exception.OrderException;
import com.space.munova.order.repository.OrderItemRepository;
import com.space.munova.order.repository.OrderRepository;
import com.space.munova.order.service.processor.CouponAppliedProcessor;
import com.space.munova.order.service.processor.NoCouponProcessor;
import com.space.munova.payment.entity.Payment;
import com.space.munova.payment.exception.PaymentException;
import com.space.munova.payment.service.PaymentService;
import com.space.munova.product.application.ProductDetailService;
import com.space.munova.recommend.service.RecommendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("Order_Service")
@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private MemberService memberService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemService orderItemService;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ProductDetailService productDetailService;
    @Mock
    private RecommendService recommendService;
    @Mock
    private CouponAppliedProcessor couponAppliedProcessor;
    @Mock
    private NoCouponProcessor noCouponProcessor;
    @Mock
    private AuthService authService;
    @Mock
    private PaymentService paymentService;

    private final Long TEST_MEMBER_ID = 1L;
    private final Long TEST_PRODUCT_ID = 1L;
    private final Long TEST_PRODUCT_DETAIL_ID = 1L;
    private final Long BASE_AMOUNT = 10000L;
    private final int PAGE_SIZE = 5;
    private final Long TEST_ORDER_ID = 100L;
    private final Long OWNER_MEMBER_ID = 1L;
    private final Long OTHER_MEMBER_ID = 2L;

    private Member mockMember;
    private Order mockOrder;
    private OrderItem mockOrderItem;
    private List<OrderItem> mockOrderItems;
    private CreateOrderRequest couponRequest;
    private CreateOrderRequest noCouponRequest;
    private Payment mockPayment;
    private List<Order> createMockOrders(int count, Long memberId) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    Order mockOrder = mock(Order.class);
                    // ID와 상태, 생성 시간은 테스트 흐름에 따라 Mocking
                    when(mockOrder.getId()).thenReturn((long) (100 + i));
                    when(mockOrder.getCreatedAt()).thenReturn(LocalDateTime.now().minusDays(i));
                    // OrderSummaryDto.from(Order)가 정상 작동하도록 필요한 필드를 Mocking할 수 있지만,
                    // 여기서는 from이 단순 매핑만 한다고 가정하고 핵심 로직에 집중합니다.
                    return mockOrder;
                })
                .toList();
    }

    @BeforeEach
    void setUp() {
        // 기본 Mock 객체 설정
        mockMember = mock(Member.class);
        mockOrder = mock(Order.class);

        // OrderItem Mock 설정
        mockOrderItem = mock(OrderItem.class);

        mockOrderItems = List.of(mockOrderItem);

        // Request 객체 설정
        couponRequest = new CreateOrderRequest(
                10L,
                "문 앞에 배송해주세요",
                9000L,
                List.of(new OrderItemRequest(TEST_PRODUCT_DETAIL_ID, 1))
        );
        noCouponRequest = new CreateOrderRequest(
                null,
                "문 앞에 배송해주세요",
                BASE_AMOUNT,
                List.of(new OrderItemRequest(TEST_PRODUCT_DETAIL_ID, 1))
        );

        mockPayment = mock(Payment.class);
    }

    // --- 1. 성공 케이스 (쿠폰 미사용) ---
    @Test
    @DisplayName("[주문생성] (HappyCase) 쿠폰이 없는 경우 NoCouponProcessor를 호출하고 주문 생성이 성공해야 한다")
    void createOrder_NoCoupon_happyCase() {
        // GIVEN
        // NoCouponProcessor.process가 예외 없이 완료됨을 가정 (doNothing()이 기본 동작)

        when(mockOrderItem.calculateAmount()).thenReturn(BASE_AMOUNT);
        when(mockOrderItem.getId()).thenReturn(50L);
        when(mockOrder.getOrderItems()).thenReturn(mockOrderItems);

        // 공통 성공 Mock 설정
        when(memberService.getMemberEntity(TEST_MEMBER_ID)).thenReturn(mockMember);
        when(orderItemService.deductStockAndCreateOrderItems(anyList(), any(Order.class)))
                .thenReturn(mockOrderItems);

        // UserActionSummary Mock
        when(orderItemRepository.findProductDetailIdsByOrderItemIds(anyList()))
                .thenReturn(List.of(TEST_PRODUCT_DETAIL_ID));
        when(productDetailService.findProductIdByDetailId(TEST_PRODUCT_DETAIL_ID))
                .thenReturn(TEST_PRODUCT_ID);
        doNothing().when(recommendService).updateUserAction(anyLong(), any(Integer.class), any(), any(), any(Boolean.class));

        try (MockedStatic<Order> staticMockOrder = mockStatic(Order.class)) {
            staticMockOrder.when(
                    () -> Order.createOrder(any(Member.class), anyString())
            ).thenReturn(mockOrder);

            // WHEN
            Order result = orderService.createOrder(noCouponRequest, TEST_MEMBER_ID);

            // THEN
            assertThat(result).isSameAs(mockOrder);
            staticMockOrder.verify(()  -> Order.createOrder(eq(mockMember), eq(noCouponRequest.userRequest())), times(1));

            // 1. 프로세서 호출 검증
            verify(noCouponProcessor, times(1)).process(
                    eq(mockOrder), eq(noCouponRequest), eq(BASE_AMOUNT)
            );
            verify(couponAppliedProcessor, never()).process(any(), any(), anyLong());

            // 2. 핵심 로직 호출 검증
            verify(memberService, times(1)).getMemberEntity(TEST_MEMBER_ID);
            verify(orderRepository, times(1)).save(result);

            // 3. UserActionSummary 로직 호출 검증
            verify(recommendService, times(1)).updateUserAction(TEST_PRODUCT_ID, 0, null, null, true);
        }
    }

    // --- 2. 성공 케이스 (쿠폰 사용) ---
    @Test
    @DisplayName("[주문생성] (HappyCase) 쿠폰이 있는 경우 CouponAppliedProcessor를 호출하고 주문 생성이 성공해야 한다")
    void createOrder_WithCoupon_happyCase() {
        // GIVEN
        // CouponAppliedProcessor.process가 예외 없이 완료됨을 가정

        when(mockOrderItem.calculateAmount()).thenReturn(BASE_AMOUNT);
        when(mockOrderItem.getId()).thenReturn(50L);
        when(mockOrder.getOrderItems()).thenReturn(mockOrderItems);

        // 공통 성공 Mock 설정
        when(memberService.getMemberEntity(TEST_MEMBER_ID)).thenReturn(mockMember);
        when(orderItemService.deductStockAndCreateOrderItems(anyList(), any(Order.class)))
                .thenReturn(mockOrderItems);

        // UserActionSummary Mock
        when(orderItemRepository.findProductDetailIdsByOrderItemIds(anyList()))
                .thenReturn(List.of(TEST_PRODUCT_DETAIL_ID));
        when(productDetailService.findProductIdByDetailId(TEST_PRODUCT_DETAIL_ID))
                .thenReturn(TEST_PRODUCT_ID);
        doNothing().when(recommendService).updateUserAction(anyLong(), any(Integer.class), any(), any(), any(Boolean.class));

        try (MockedStatic<Order> staticMockOrder = mockStatic(Order.class)) {
            staticMockOrder.when(
                    () -> Order.createOrder(any(Member.class), anyString())
            ).thenReturn(mockOrder);
            // WHEN
            Order result = orderService.createOrder(couponRequest, TEST_MEMBER_ID);

            // THEN
            assertThat(result).isSameAs(mockOrder);

            staticMockOrder.verify(() -> Order.createOrder(eq(mockMember), eq(couponRequest.userRequest())), times(1));

            // 1. 프로세서 호출 검증
            verify(couponAppliedProcessor, times(1)).process(
                    eq(mockOrder), eq(couponRequest), eq(BASE_AMOUNT)
            );
            verify(noCouponProcessor, never()).process(any(), any(), anyLong());

            // 2. 핵심 로직 호출 검증
            verify(orderRepository, times(1)).save(mockOrder);
            verify(recommendService, times(1)).updateUserAction(TEST_PRODUCT_ID, 0, null, null, true);
        }
    }

    //     --- 3. 예외 케이스 ---
    @Test
    @DisplayName("프로세서에서 금액 불일치 예외 발생 시 주문 저장이 되면 안 된다")
    void createOrder_ProcessorThrowsException_ShouldNotSaveOrder() {
        // GIVEN
        // NoCouponProcessor가 예외를 던지도록 Mock 설정
        doThrow(OrderException.amountMismatchException()).when(noCouponProcessor).process(
                any(Order.class), any(CreateOrderRequest.class), anyLong()
        );

        // WHEN & THEN
        assertThatThrownBy(() -> orderService.createOrder(noCouponRequest, TEST_MEMBER_ID))
                .isInstanceOf(OrderException.class);

        // 주문 저장 및 UserActionSummary 로직이 호출되지 않았는지 검증
        verify(orderRepository, never()).save(any());
        verify(recommendService, never()).updateUserAction(anyLong(), any(Integer.class), any(), any(), any(Boolean.class));
    }

    // --- 1. Happy Case: 데이터가 정상적으로 존재할 때 ---
    @Test
    @DisplayName("[전체조회] (HappyCase) 데이터가 존재할 경우, 올바른 페이징 정보와 OrderSummaryDto 목록을 반환해야 한다")
    void getOrderList_Success() {
        // GIVEN
        int page = 0;
        int totalElements = 25;
        List<Order> mockPageContent = createMockOrders(PAGE_SIZE, TEST_MEMBER_ID); // 5개
        List<Long> expectedOrderIds = mockPageContent.stream().map(Order::getId).toList();

        // Pageable 검증을 위한 캡처
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        // 1. findAllByMember_IdAndStatus Mocking: 첫 번째 쿼리 (페이징된 ID 조회)
        Page<Order> mockOrderPage = new PageImpl<>(mockPageContent, PageRequest.of(page, PAGE_SIZE), totalElements);
        when(orderRepository.findAllByMember_IdAndStatus(
                eq(TEST_MEMBER_ID),
                eq(OrderStatus.PAID),
                any(Pageable.class)
        )).thenReturn(mockOrderPage);

        // 2. findAllWithDetailsByOrderIds Mocking: 두 번째 쿼리 (상세 정보 배치 조회)
        // 실제 OrderSummaryDto로 변환될 수 있도록 같은 내용의 List를 반환
        when(orderRepository.findAllWithDetailsByOrderIds(expectedOrderIds))
                .thenReturn(mockPageContent);

        // OrderSummaryDto::from이 정상적으로 OrderSummaryDto 목록을 만든다고 가정

        // WHEN
        PagingResponse<OrderSummaryDto> response = orderService.getOrderList(page, TEST_MEMBER_ID);

        // THEN
        // 1. Pageable 검증 (Sort, Page, Size)
        verify(orderRepository).findAllByMember_IdAndStatus(
                eq(TEST_MEMBER_ID),
                eq(OrderStatus.PAID),
                pageableCaptor.capture()
        );
        Pageable actualPageable = pageableCaptor.getValue();
        assertThat(actualPageable.getPageNumber()).isEqualTo(page);
        assertThat(actualPageable.getPageSize()).isEqualTo(PAGE_SIZE);
        assertThat(actualPageable.getSort().toString()).contains("createdAt: DESC");

        // 2. 메서드 호출 순서 및 횟수 검증 (N+1 방지 로직 실행 확인)
        verify(orderRepository, times(1)).findAllByMember_IdAndStatus(anyLong(), any(OrderStatus.class), any(Pageable.class));
        verify(orderRepository, times(1)).findAllWithDetailsByOrderIds(eq(expectedOrderIds));

        // 3. PagingResponse 결과 검증
        assertThat(response.content()).hasSize(PAGE_SIZE);
        assertThat(response.totalElements()).isEqualTo(totalElements);
        assertThat(response.totalPages()).isEqualTo((int) Math.ceil((double) totalElements / PAGE_SIZE));
        assertThat(response.page()).isEqualTo(page);
    }

    // --- 2. Empty Case: 조회된 주문이 없는 경우 ---
    @Test
    @DisplayName("[전체조회] (HappyCase) 조회된 주문이 없을 경우, 빈 PagingResponse를 반환해야 한다")
    void getOrderList_EmptyCase() {
        // GIVEN
        int page = 0;
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 1. findAllByMember_IdAndStatus Mocking: 빈 Page 반환
        Page<Order> emptyOrderPage = Page.empty(pageable);
        when(orderRepository.findAllByMember_IdAndStatus(
                anyLong(),
                any(OrderStatus.class),
                any(Pageable.class)
        )).thenReturn(emptyOrderPage);

        // WHEN
        PagingResponse<OrderSummaryDto> response = orderService.getOrderList(page, TEST_MEMBER_ID);

        // THEN
        // 1. 두 번째 쿼리(findAllWithDetailsByOrderIds)는 호출되지 않아야 함
        verify(orderRepository, times(1)).findAllByMember_IdAndStatus(anyLong(), any(OrderStatus.class), any(Pageable.class));
        verify(orderRepository, never()).findAllWithDetailsByOrderIds(anyList());

        // 2. PagingResponse 결과 검증
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.page()).isEqualTo(page);
    }

    @Test
    @DisplayName("[상세조회] (HappyCase) 권한 검증 성공 후, 상세 정보를 정상적으로 조회해야 한다")
    void getOrderDetail_AuthorizationSuccess_HappyCase() {
        // GIVEN
        when(mockMember.getId()).thenReturn(OWNER_MEMBER_ID);
        when(mockOrder.getMember()).thenReturn(mockMember);

        // 1. Order 조회 성공
        when(orderRepository.findOrderDetailsById(TEST_ORDER_ID))
                .thenReturn(Optional.of(mockOrder));

        // 2. AuthService.verifyAuthorization은 예외 없이 통과한다고 설정 (doNothing()이 기본 동작)
         doNothing().when(authService).verifyAuthorization(eq(OWNER_MEMBER_ID), anyLong()); // 명시적으로 작성해도 됨

        // 3. Payment 조회 성공
        when(paymentService.getPaymentByOrderId(TEST_ORDER_ID))
                .thenReturn(mockPayment);

        // WHEN
        orderService.getOrderDetail(TEST_ORDER_ID, OWNER_MEMBER_ID);

        // THEN
        // 1. OrderRepository와 PaymentService 호출 검증
        verify(orderRepository, times(1)).findOrderDetailsById(TEST_ORDER_ID);
        verify(paymentService, times(1)).getPaymentByOrderId(TEST_ORDER_ID);

        // 2. AuthService.verifyAuthorization이 올바른 인자(주문 소유자 ID, 현재 사용자 ID)로 호출되었는지 검증
        verify(authService, times(1)).verifyAuthorization(
                eq(OWNER_MEMBER_ID), // 주문 소유자 ID
                eq(OWNER_MEMBER_ID)  // 현재 사용자 ID
        );
    }

    // --- 2. 실패 케이스: 주문 존재 여부 ---
    @Test
    @DisplayName("[상세조회] 존재하지 않는 orderId로 조회 시 OrderException이 발생해야 한다")
    void getOrderDetail_OrderNotFound_ShouldThrowException() {
        // GIVEN
        when(orderRepository.findOrderDetailsById(TEST_ORDER_ID))
                .thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> orderService.getOrderDetail(TEST_ORDER_ID, OWNER_MEMBER_ID))
                .isInstanceOf(OrderException.class);

        // AuthService와 PaymentService는 호출되지 않아야 함
        verify(authService, never()).verifyAuthorization(anyLong(), anyLong());
        verify(paymentService, never()).getPaymentByOrderId(anyLong());
    }

    // --- 3. 실패 케이스: 권한 문제 (AuthService 예외 전파 검증) ---
    @Test
    @DisplayName("[상세조회] 권한 검증 실패 시 AuthService의 AuthException이 그대로 전파되어야 한다")
    void getOrderDetail_AuthorizationFailure_ShouldThrowAuthException() {
        // GIVEN
        when(mockMember.getId()).thenReturn(OWNER_MEMBER_ID);
        when(mockOrder.getMember()).thenReturn(mockMember);

        // 1. Order 조회 성공
        when(orderRepository.findOrderDetailsById(TEST_ORDER_ID))
                .thenReturn(Optional.of(mockOrder));

        // 2. AuthService가 AuthException을 던지도록 Mock 설정 (권한 없음 시나리오)
        doThrow(AuthException.unauthorizedException())
                .when(authService).verifyAuthorization(
                        eq(OWNER_MEMBER_ID), // 주문 소유자
                        eq(OTHER_MEMBER_ID)  // 다른 사용자
                );

        // WHEN & THEN
        assertThatThrownBy(() -> orderService.getOrderDetail(TEST_ORDER_ID, OTHER_MEMBER_ID))
                .isInstanceOf(AuthException.class);

        // PaymentService는 권한 검증 실패 후 호출되지 않아야 함
        verify(paymentService, never()).getPaymentByOrderId(anyLong());

        // OrderRepository 호출은 성공했음을 검증
        verify(orderRepository, times(1)).findOrderDetailsById(TEST_ORDER_ID);
    }

    // --- 4. 실패 케이스: Payment 문제 (PaymentService 예외 전파 검증) ---
    @Test
    @DisplayName("[상세조회] 존재하지 않는 orderId로 Payment 조회시 PaymentService의 PaymentException이 그대로 전파되어야 한다")
    void getOrderDetail_NoPayment_ShouldThrowPaymentException() {
        // GIVEN
        when(mockMember.getId()).thenReturn(OWNER_MEMBER_ID);
        when(mockOrder.getMember()).thenReturn(mockMember);

        when(orderRepository.findOrderDetailsById(TEST_ORDER_ID))
                .thenReturn(Optional.of(mockOrder));

        doNothing().when(authService).verifyAuthorization(eq(OWNER_MEMBER_ID), anyLong()); // 명시적으로 작성해도 됨

        // 2. PaymentService가 PaymentException을 던지도록 Mock 설정
        doThrow(PaymentException.orderMismatchException())
                .when(paymentService).getPaymentByOrderId(TEST_ORDER_ID);

        // WHEN & THEN
        assertThatThrownBy(() -> orderService.getOrderDetail(TEST_ORDER_ID, OTHER_MEMBER_ID))
                .isInstanceOf(PaymentException.class);

        verify(orderRepository, times(1)).findOrderDetailsById(TEST_ORDER_ID);
        verify(authService, times(1)).verifyAuthorization(anyLong(), anyLong());
        verify(paymentService, times(1)).getPaymentByOrderId(TEST_ORDER_ID);
    }
}
