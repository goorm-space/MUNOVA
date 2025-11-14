package com.space.munova.recommend.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.recommend.dto.RecommendProductResponseDto;
import com.space.munova.recommend.dto.RecommendReasonResponseDto;
import com.space.munova.recommend.infra.RedisStreamProducer;
import com.space.munova.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;
    private final RedisStreamProducer producer;

    //test3
//    @PostMapping("/recommend/logtest/{productId}")
//    public ResponseEntity<String> test3(@PathVariable Long  productId) {
//        Map<String, Object> logData = Map.of(
//                "event_time", "2025-11-07T14:12:31.123Z",
//                "event_type", "product_create",
//                "service", "product",
//                "member_id", 1001,
//                "session_id", "2a1b3c4d-1234-5678-9876-abcde",
//                //"user_agent", request.getHeader("User-Agent"), // ✅ 요청 헤더에서 자동 추출
//                "version", 1,
//                "data", Map.of(
//                        "product_id", productId,
//                        "view_count", 2203
//                )
//        );
//
//        RecordId id = producer.sendLog(logData);
//
//        return ResponseEntity.ok("✅ Log sent to Redis Stream. Record ID: " + id);
//
//    }


    // test2
    @PostMapping("/recommend/test/{productId}")
    public ResponseEntity<String> test2(@PathVariable Long productId){
    //    producer.sendUserAction("test",productId);
        return ResponseEntity.ok().body("test");
    }

    @PutMapping("/api/recommend/user/{productId}")
    public ResponseEntity<ResponseApi<List<FindProductResponseDto>>> updateMemberProductRecommend(@PathVariable Long productId) {
        return recommendService.updateUserProductRecommend(productId);
    }

    @PutMapping("/recommend/{productId}")
    public ResponseEntity<ResponseApi<List<FindProductResponseDto>>> updateSimilarProductRecommend(@PathVariable Long productId) {
        return recommendService.updateSimilarProductRecommend(productId);
    }

    @GetMapping("/api/admin/recommend/user/{userId}/product/{productId}/based_on")
    public ResponseEntity<ResponseApi<List<RecommendReasonResponseDto>>> getRecommendationReason(@PathVariable Long userId, @PathVariable Long productId,@PageableDefault(size = 10, sort="CreatedAt") Pageable pageable) {
        List<RecommendReasonResponseDto> reason=recommendService.getRecommendationReason(userId, productId);
        return ResponseEntity.ok().body(ResponseApi.ok(reason));
    }

    @GetMapping("/api/admin/recommend/user/{userId}/product/{productId}/score")
    public double getRecommendationScore(@PathVariable Long userId, @PathVariable Long productId) {
        return recommendService.getRecommendationScore(userId, productId);
    }
}