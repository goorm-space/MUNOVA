package com.space.munova.product.infra.redis;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;

///  상품의 좋아요, 판매량, 조회수 등등 1시간마다
/// RDB, MONGO, ES 에 업데이트 하기 위한 데이터
@RedisHash("product-stats-")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ProductRedisStatsData {

    ///  상품아이디
    @Id
    private String id;

    private Integer viewCount;

    private Integer salesCount;

    private Integer likeCount;
}
