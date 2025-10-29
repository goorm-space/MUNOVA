package com.space.munova.recommend.service;

import com.space.munova.recommend.domain.UserActionSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none" // DDL 실행을 막는 설정 주입
})
public class RecommendScoreServiceTest {

    @Autowired
    private RecommendService recommendService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testRecommendationScore() {
        Long userId = 1L;
        Long productId = 101L;

        // 1️⃣ 유저 행동 업데이트
        recommendService.updateUserAction(productId, 1, true, false, true);

        // 2️⃣ Redis에서 데이터 가져오기
        UserActionSummary summary = (UserActionSummary) redisTemplate
                .opsForValue()
                .get("user:action:" + userId + ":" + productId);

        System.out.println("Redis에 저장된 데이터: " + summary);

        // 3️⃣ 추천 점수 계산
        double score = recommendService.getRecommendationScore(userId, productId);
        System.out.println("추천 점수: " + score);
    }
}