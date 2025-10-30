package com.space.munova.recommend.service;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.recommend.dto.RecommendReasonResponseDto;
import com.space.munova.recommend.dto.RecommendationsProductResponseDto;
import com.space.munova.recommend.dto.RecommendationsUserResponseDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface RecommendService {

    List<RecommendationsUserResponseDto> getRecommendationsByMemberId(Long memberId);

    List<RecommendationsProductResponseDto> getRecommendationsByProductId(Long productId);

    ResponseEntity<ResponseApi<List<FindProductResponseDto>>> updateUserProductRecommend(Long productId);
    ResponseEntity<ResponseApi<List<FindProductResponseDto>>> updateSimilarProductRecommend(Long productId);

    List<RecommendReasonResponseDto> getRecommendationReason(Long userId, Long productId);
    double getRecommendationScore(Long userId, Long productId);
    void updateUserAction( Long productId, Integer clicked, Boolean liked, Boolean inCart, Boolean purchased);
}