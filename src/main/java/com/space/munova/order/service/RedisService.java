package com.space.munova.order.service;

import com.space.munova.order.dto.redis.TmpOrderDto;
import com.space.munova.order.dto.redis.TmpOrderItemDto;
import com.space.munova.order.exception.OrderException;
import com.space.munova.product.application.product.command.ProductDetailService;
import com.space.munova.product.application.product.command.exception.ProductDetailException;
import com.space.munova.product.domain.ProductDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class RedisService {

    private final static String STOCK_KEY = "stock:productDetail:";
    private final static String TEMP_ORDER_KEY = "order:temp:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, Integer> integerRedisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedisTemplate<String, TmpOrderDto> tmpOrderRedisTemplate;

    private final ProductDetailService productDetailService;

    String decreaseStockLuaScript = """
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

    String restoreStockLuaScript = """
                    local n = #KEYS
                    for i = 1, n do
                        local key = KEYS[i]
                        local quantity = tonumber(ARGV[i])
                        redis.call('INCRBY', key, quantity)
                    end
                    return 1
            """;

    /**
     * 재고 검증 및 차감
     * (lua script로 atomic 연산 보장)
     */
    public void validateAndDecreaseStock(List<TmpOrderItemDto> orderItems) {
        List<String> keys = orderItems.stream()
                .map(req -> STOCK_KEY + req.productDetailId())
                .collect(Collectors.toList());

        Object[] args = orderItems.stream()
                .map(req -> String.valueOf(req.quantity()))
                .toArray();

        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(decreaseStockLuaScript);
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
     * 결제 전 재고 검증
     */
    public void validateStock(Long productDetailId, int quantity) {
        String key = STOCK_KEY + productDetailId;
        Integer stock = integerRedisTemplate.opsForValue().get(key);

        // 캐시에 없으면
        if (stock == null) {
            // 데이터베이스에서 읽고, 캐시에 업데이트
            ProductDetail productDetail = productDetailService.getStock(productDetailId, quantity);
            saveStock(productDetailId, productDetail.getQuantity());
        } else if (stock == 0) {
            throw ProductDetailException.noStockException("REDIS_KEY: " + STOCK_KEY + productDetailId);
        } else if (stock < quantity) {
            throw ProductDetailException.stockInsufficientException("REDIS_KEY: " + STOCK_KEY + productDetailId + ", 요청: " + quantity + ", 재고: " + stock);
        }
    }

    /**
     * 재고 차감
     */
    public void decreaseStock(Long productDetailId, int quantity) {
        String key = STOCK_KEY + productDetailId;

        redisTemplate.opsForValue().decrement(key, quantity);
    }

    /**
     * 재고 복구
     */
    public void restoreStock(List<TmpOrderItemDto> orderItems) {
        List<String> keys = orderItems.stream()
                .map(req -> STOCK_KEY + req.productDetailId())
                .collect(Collectors.toList());

        Object[] args = orderItems.stream()
                .map(req -> String.valueOf(req.quantity()))
                .toArray();

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(restoreStockLuaScript);
        script.setResultType(Long.class);

        stringRedisTemplate.execute(script, keys, args);
    }


    /**
     * 임시 주문서 저장
     */
    public void saveTemporaryOrder(TmpOrderDto tmpOrder) {
        tmpOrderRedisTemplate.opsForValue().set(
                TEMP_ORDER_KEY + tmpOrder.orderNum(), tmpOrder, Duration.ofMinutes(5)
        );
    }

    public TmpOrderDto getTemporaryOrder(String orderNum) {
        TmpOrderDto tmpOrder = tmpOrderRedisTemplate.opsForValue().get(TEMP_ORDER_KEY + orderNum);

        if (tmpOrder == null) {
            throw OrderException.notFoundException("주문이 만료되었거나 존재하지 않습니다.");
        }
        return tmpOrder;
    }

    /**
     * 임시 주문서 삭제
     */
    public void deleteTmpOrder(String orderNum) {
        redisTemplate.delete(TEMP_ORDER_KEY + orderNum);
    }

    /**
     * 재고 저장
     */
    public void saveStock(Long productDetailId, int quantity) {
        integerRedisTemplate.opsForValue().set(STOCK_KEY + productDetailId, quantity);
    }
}
