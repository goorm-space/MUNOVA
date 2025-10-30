package com.space.munova.recommend.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.recommend.dto.RecommendReasonResponseDto;
import com.space.munova.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @PutMapping("/api/recommend/user/{productId}")
    public ResponseEntity<ResponseApi<List<FindProductResponseDto>>> updateMemberProductRecommend(@PathVariable Long productId) {
        return recommendService.updateUserProductRecommend(productId);
    }

    @PutMapping("/recommend/{productId}")
    public ResponseEntity<ResponseApi<List<FindProductResponseDto>>> updateSimilarProductRecommend(@PathVariable Long productId) {
        return recommendService.updateSimilarProductRecommend(productId);
    }

    @GetMapping("/api/admin/recommend/user/{userId}/product/{productId}/based_on")
    public List<RecommendReasonResponseDto> getRecommendationReason(@PathVariable Long userId, @PathVariable Long productId) {
        return recommendService.getRecommendationReason(userId, productId);
    }

    @GetMapping("/api/admin/recommend/user/{userId}/product/{productId}/score")
    public double getRecommendationScore(@PathVariable Long userId, @PathVariable Long productId) {
        return recommendService.getRecommendationScore(userId, productId);
    }
}