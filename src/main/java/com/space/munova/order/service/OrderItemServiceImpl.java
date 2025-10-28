package com.space.munova.order.service;

import com.space.munova.auth.exception.AuthException;
import com.space.munova.order.dto.CancelOrderItemRequest;
import com.space.munova.order.dto.CancelType;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.exception.OrderItemException;
import com.space.munova.order.repository.OrderItemRepository;
import com.space.munova.payment.service.PaymentService;
import com.space.munova.product.application.ProductDetailService;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final ProductDetailService productDetailService;
    private final PaymentService paymentService;

    @Transactional
    @Override
    public void cancelOrderItem(Long orderItemId, CancelOrderItemRequest request) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(OrderItemException::notFoundException);

        Long userId = JwtHelper.getMemberId();

        validateCancellation(userId, orderItem, request.cancelType());

        paymentService.cancelPaymentAndSaveRefund(orderItem, orderItem.getOrder().getId(), request);

        restoreProductDetailStock(orderItem);

        if (request.cancelType().equals(CancelType.ORDER_CANCEL)) {
            orderItem.updateStatus(OrderStatus.CANCELED);
        } else if (request.cancelType().equals(CancelType.RETURN_REFUND)) {
            orderItem.updateStatus(OrderStatus.REFUNDED);
        }

    }

    /**
     * 취소 가능 여부 유효성 검사
     */
    private void validateCancellation(Long userId, OrderItem orderItem, CancelType type) {
        if (!userId.equals(orderItem.getOrder().getMember().getId())) {
            throw AuthException.unauthorizedException(
                    "접근 시도한 userId:", userId.toString(),
                    "orderItemId:", orderItem.getId().toString()
            );
        }

        OrderStatus currentStatus = orderItem.getStatus();
        switch (type) {
            case ORDER_CANCEL:
                if (currentStatus != OrderStatus.PAID) {
                    throw OrderItemException.cancellationNotAllowedException(
                            String.format("주문 취소는 'PAID' 상태에서만 가능합니다. 현재 상태: %s", currentStatus)
                    );
                }
                break;

            case RETURN_REFUND:
                if (currentStatus != OrderStatus.SHIPPING && currentStatus != OrderStatus.DELIVERED) {
                    throw OrderItemException.cancellationNotAllowedException(
                            String.format("반품은 'SHIPPING' 또는 'DELIVERED' 상태에서만 가능합니다. 현재 상태: %s", currentStatus)
                    );
                }
                break;

            default:
                throw OrderItemException.cancellationNotAllowedException("정의되지 않은 취소 유형입니다.");
        }
    }

    /**
     * OrderItem에 해당하는 상품 재고 복구
     */
    private void restoreProductDetailStock(OrderItem orderItem) {
        Long productDetailId = orderItem.getProductDetail().getId();
        int cancelQuantity = orderItem.getQuantity();

        productDetailService.restoreProductDetailStock(productDetailId, cancelQuantity);

    }
}
