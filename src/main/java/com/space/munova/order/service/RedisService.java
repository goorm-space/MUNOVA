package com.space.munova.order.service;

import com.space.munova.order.dto.OrderItemRequest;
import com.space.munova.order.dto.redis.TmpOrderDto;
import com.space.munova.order.entity.Order;
import com.space.munova.order.exception.OrderException;
import com.space.munova.product.application.product.command.ProductDetailService;
import com.space.munova.product.application.product.command.exception.ProductDetailException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class RedisService {

    private final static String STOCK_KEY = ":stock";
    private final static String TEMP_ORDER_KEY = "order:temp:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, Integer> integerRedisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate;

    private final ProductDetailService productDetailService;

    String luaScriptText = """
                    local n = #KEYS
            
                    for i = 1, n do
                        local key = KEYS[i]
                        local quantity = tonumber (ARGV[i])
                        local stock = tonumber (redis.call('GET', key) or 0)
                        if stock < quantity then
                            return {0, i-1, stock}
                        end
                    end
            
                    for i = 1, n do
                        local key = KEYS[i]
                        local quantity = tonumber(ARGV[i])
                        redis.call('DECRBY', key, quantity)
                    end
            
                    return {1}
            """;

    /**
     * lua script로 atomic 연산 보장
     */
    public void validateAndDecreaseStock(List<OrderItemRequest> orderItems) {
        List<String> keys = orderItems.stream()
                .map(req -> "productDetail:" + req.productDetailId() + STOCK_KEY)
                .collect(Collectors.toList());

        Object[] args = orderItems.stream()
                .map(req -> String.valueOf(req.quantity()))
                .toArray();

        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(luaScriptText);
        script.setResultType(List.class);

        List<Object> result = stringRedisTemplate.execute(script, keys, args);

        if (result == null || result.isEmpty()) {
            throw new RuntimeException();
        }

        Number isSuccess = (Number) result.get(0);
        if (isSuccess.intValue() != 1) {
            int idx = ((Number) result.get(1)).intValue();
            int currentStock = ((Number) result.get(2)).intValue();

            if (currentStock == 0) {
                throw ProductDetailException.noStockException("product_detail_id: " + orderItems.get(idx).productDetailId());
            } else {
                throw ProductDetailException.stockInsufficientException("product_detail_id: " + orderItems.get(idx).productDetailId() + ", 요청: " + orderItems.get(idx).quantity() + ", 재고: " + currentStock);
            }
        }
    }

    /**
     * 재고 검증
     */
    public void validateStock(Long productDetailId, int quantity) {
        String key = "productDetail:" + productDetailId + STOCK_KEY;
        Integer stock = integerRedisTemplate.opsForValue().get(key);

        if (stock == null || stock == 0) {
            throw ProductDetailException.noStockException("product_detail_id: " + productDetailId);
        } else if (stock < quantity) {
            throw ProductDetailException.stockInsufficientException("product_detail_id: " + productDetailId + ", 요청: " + quantity + ", 재고: " + stock);
        }
    }

    /**
     * 재고 차감
     */
    public void decreaseStock(Long productDetailId, int quantity) {
        String key = "productDetail:" + productDetailId + STOCK_KEY;

        redisTemplate.opsForValue().decrement(key, quantity);
    }


    /**
     * 임시 주문서 저장
     */
    public void saveTemporaryOrder(TmpOrderDto tmpOrder) {
        redisTemplate.opsForValue().set(
                TEMP_ORDER_KEY + tmpOrder.orderNum(), tmpOrder, Duration.ofMinutes(5)
        );
    }

    public Order getTemporaryOrder(String orderNum) {
        Order tmpOrder = (Order) redisTemplate.opsForValue().get(TEMP_ORDER_KEY + orderNum);

        if (tmpOrder == null) {
            throw OrderException.notFoundException("임시 주문이 만료되었거나 존재하지 않습니다.");
        }
        return tmpOrder;
    }

    public void deleteTmpOrder(String orderNum) {
        redisTemplate.delete(TEMP_ORDER_KEY + orderNum);
    }
}
