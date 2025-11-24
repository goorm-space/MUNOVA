package com.space.munova.order.scheduler;

import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.repository.OrderRepository;
import com.space.munova.product.application.ProductDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderRepository orderRepository;
    private final ProductDetailService productDetailService;

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void rollbackStockWhenPaymentPending() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);

        List<OrderStatus> targetStatuses = List.of(
                OrderStatus.CREATED,
                OrderStatus.PAYMENT_PENDING
        );

        List<Order> orders = orderRepository.findByStatusInAndCreatedAtBefore(targetStatuses, cutoff);

        if (orders.isEmpty()) {
            log.info("스케줄러: 처리할 주문 없음");
            return;
        }

        log.info("스케줄러: 처리할 주문 수 = {}", orders.size());

        for (Order order : orders) {
            try {
                if (order.getStatus() == OrderStatus.PAYMENT_FAILED) continue;

                for (OrderItem item : order.getOrderItems()) {
                    Long productDetailId = item.getProductDetail().getId();
                    int quantity = item.getQuantity();

                    productDetailService.increaseStock(productDetailId, quantity);
                    log.info("스케줄러: 상품 재고 복원 productId={} quantity={}", productDetailId, quantity);
                }

                order.updateStatus(OrderStatus.PAYMENT_FAILED);
                orderRepository.save(order);
                log.info("스케줄러: 주문 상태 변경 완료 id={}", order.getId());
            } catch (Exception e) {
                log.error("스케줄러: 주문 rollback 실패 id={} msg={}", order.getId(), e.getMessage(), e);
            }
        }
    }
}
