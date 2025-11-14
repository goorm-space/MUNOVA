package com.space.munova.order.service;

import com.space.munova.auth.exception.AuthException;
import com.space.munova.order.dto.CancelOrderItemRequest;
import com.space.munova.order.dto.CancelType;
import com.space.munova.order.dto.OrderItemRequest;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.exception.OrderItemException;
import com.space.munova.order.repository.OrderItemRepository;
import com.space.munova.payment.service.PaymentService;
import com.space.munova.product.application.ProductDetailService;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.recommend.service.RecommendService;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final ProductDetailService productDetailService;
    private final PaymentService paymentService;
    private final RecommendService recommendService;

    @Override
    public List<OrderItem> deductStockAndCreateOrderItems(List<OrderItemRequest> itemRequests, Order order) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw OrderItemException.noOrderItemsNotAllowedException();
        }

        List<OrderItem> orderItems = new ArrayList<>();

        for(OrderItemRequest orderItemRequest : itemRequests) {
            ProductDetail detail = productDetailService.deductStock(orderItemRequest.productDetailId(), orderItemRequest.quantity());

            // 2. Orderitem 엔티티 생성
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productDetail(detail)
                    .nameSnapshot(detail.getProduct().getName())
                    .priceSnapshot(detail.getProduct().getPrice())
                    .quantity(orderItemRequest.quantity())
                    .status(OrderStatus.CREATED)
                    .build();

            orderItems.add(orderItem);
        }

        return orderItems;
    }

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

        List<Long> singleOrderItemId= List.of(orderItemId);
        List<Long> productDetailId=orderItemRepository.findProductDetailIdsByOrderItemIds(singleOrderItemId);
        Long productId=productDetailService.findProductIdByDetailId(productDetailId.get(0));
        recommendService.updateUserAction(productId,0,null,null,false);
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
