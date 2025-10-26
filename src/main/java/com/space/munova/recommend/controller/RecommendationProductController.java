package com.space.munova.recommend.controller;

import com.space.munova.recommend.dto.RecommendationsProductResponseDto;
import com.space.munova.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/recommend/products")
@RequiredArgsConstructor
public class RecommendationProductController {

    private final RecommendService recommendService;

    //전체 상품 기반 추천 로그
    @GetMapping()
    public List<RecommendationsProductResponseDto> getAllProductRecommendations() {
        return recommendService.getRecommendationsByProductId(null);
    }
    //{productId}의 상품 기반 추천 로그
    @GetMapping("/{productId}")
    public List<RecommendationsProductResponseDto> getProductRecommendations(@PathVariable Long productId) {
        return recommendService.getRecommendationsByProductId(productId);
    }
}