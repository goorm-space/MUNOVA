package com.space.munova.product.infra.redis.command;

import com.space.munova.product.application.product.command.exception.ProductException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.Collections;

@Repository
public class ProductStatsRedisDataCommandRepoImpl implements ProductStatsRedisDataCommandRepo {

   private final RedisTemplate<String, Object> redisTemplate;

   /// 버전별 상품 통계 관리위한 레디스 키 (맵으로 관리)
   private static final String PRODUCT_STATS = "product-stats";

    ///  롱타입으로 반환.
    private final DefaultRedisScript<Long> updateFieldScript;

   /// 루아스크립트로 원자적으로 연산처리  -> 하나의 동시성 보장을 위해서  작성. -> 데이터를 한번에 처리하기위해
   private static final String UPDATE_FIELD_SCRIPT =
           "local statsKey = KEYS[1]; " +                                                                 // 레디스 키 -> product-stats: {productId}
                   "local field = ARGV[1]; " +                                                            // 업데이트 필드명 -> likeCount, viewCount 등등.
                   "local input = tonumber(ARGV[2]); " +                                                  // 인풋값 -> 1 or -1
                   "if redis.call('EXISTS', statsKey) == 0 then " +                                       // 키값 존재 확인 -> 1있음 0없음
                   "redis.call('HSET', statsKey, 'likeCount', 0, 'viewCount', 0, 'salesCount', 0); " +    // 해쉬에 초기값을 저장.  product-stats: {productId}
                                                                                                          //  map {likecount : 0, viewCount:0, salesCount:0}
                   "end; " +
                   "local current = tonumber(redis.call('HGET', statsKey, field)) or 0; " +               // 요청한 필드 값을 조회. tonumber() 함수 - 문자열 형식 -> 숫자로변경
                                                                                                          // 만약 null을 반환하면 0으로 아니면 값을 가져온다.
                   "local newValue = current + input; " +                                                 // 값을 더해준다.
                   "if newValue < 0 then " +                                                              // 결과값이 0보다 작을경우 에러 보내준다.
                   "return -1   " +
                   "end; " +
                   "redis.call('HSET', statsKey, field, newValue); " +
                   "return newValue ";                                                                    // 최종업데이트 결과 반환.

    ///  레디스 템플릿 생성자.
    public ProductStatsRedisDataCommandRepoImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;

        this.updateFieldScript = new DefaultRedisScript<>();
        this.updateFieldScript.setScriptText(UPDATE_FIELD_SCRIPT);
        this.updateFieldScript.setResultType(Long.class);
    }

    /// 증가
    @Override
    public Long incrementSalesCount(Long productId, int input) {

       return updateField(productId, "salesCount", input);
    }

    @Override
    public Long incrementLikeCount(Long productId, int input) {

        return updateField(productId, "likeCount", input);
    }

    @Override
    public Long incrementViewCount(Long productId, int input) {

        return updateField(productId, "viewCount", input);
    }

    ///  감소
    @Override
    public Long decrementLikeCount(Long productId, int input) {

        return updateField(productId, "likeCount", input);
    }


    /// ============================== 내부 로직 =================================///

    /// 업데이트 메서드
    private Long updateField(Long productId, String field, int input) {
        String key = PRODUCT_STATS + productId;
        Long result = redisTemplate.execute(
                updateFieldScript,
                Collections.singletonList(key),
                field,
                String.valueOf(input)
        );

        if (result == -1) {
            throw ProductException.badRequestException("잘못된 요청입니다.");
        }

        return result;
    }

}
