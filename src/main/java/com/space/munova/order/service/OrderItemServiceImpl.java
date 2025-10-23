package com.space.munova.order.service;

import com.space.munova.auth.exception.AuthException;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.exception.OrderItemException;
import com.space.munova.order.repository.OrderItemRepository;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.product.exception.ProductDetailException;
import com.space.munova.security.jwt.JwtHelper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;

    @Override
    public void updateStatusAndCancel(Long orderItemId) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(OrderItemException::notFoundException);

        Long userId = JwtHelper.getMemberId();
        validateCancellation(userId, orderItem);

        // Todo: 결제 취소 로직

        restoreStock(orderItem);

        orderItem.updateStatus(OrderStatus.CANCELED);
    }

    /**
     * 취소 가능 여부 유효성 검사
     */
    private void validateCancellation(Long userId, OrderItem orderItem) {
        if (!userId.equals(orderItem.getOrder().getMember().getId())) {
            throw AuthException.unauthorizedException(
                    "접근 시도한 userId:", userId.toString(),
                    "orderItemId:", orderItem.getId().toString()
            );
        }

        if (orderItem.getStatus() != OrderStatus.PAID) {
            throw OrderItemException.cancellationNotAllowedException("현재 상태: " + orderItem.getStatus());
        }
    }

    /**
     * OrderItem에 해당하는 상품 재고 복구
     */
    private void restoreStock(OrderItem orderItem) {
        ProductDetail productDetail = orderItem.getProductDetail();
        Integer cancelQuantity = orderItem.getQuantity();

        int currentStock = productDetail.getQuantity() != null ? productDetail.getQuantity() : 0;
        int newStock = currentStock + cancelQuantity;
        System.out.println(currentStock + "에서 " + newStock + "으로 재고 증가 완료");
//        productDetail.setQuantity(currentStock + cancelQuantity);
    }
}
