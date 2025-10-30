package com.space.munova.recommend.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.recommend.dto.RecommendationsProductResponseDto;
import com.space.munova.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/recommend/products")
@RequiredArgsConstructor
public class RecommendationProductController {

    private final RecommendService recommendService;

    //전체 상품 기반 추천 로그
    @GetMapping()
    public ResponseEntity<ResponseApi<PagingResponse<RecommendationsProductResponseDto>>> getAllProductRecommendations(@PageableDefault(size = 10, sort="CreatedAt") Pageable pageable) {
        PagingResponse<RecommendationsProductResponseDto> recommendations = recommendService.getRecommendationsByProductId(null, pageable);

        return ResponseEntity.ok().body(ResponseApi.ok(recommendations));
    }
    //{productId}의 상품 기반 추천 로그
    @GetMapping("/{productId}")
    public ResponseEntity<ResponseApi<PagingResponse<RecommendationsProductResponseDto>>> getProductRecommendations(@PathVariable Long productId,@PageableDefault(size = 10, sort="CreatedAt") Pageable pageable) {
        PagingResponse<RecommendationsProductResponseDto> recommendations = recommendService.getRecommendationsByProductId(productId, pageable);
        return ResponseEntity.ok().body(ResponseApi.ok(recommendations));
    }
}