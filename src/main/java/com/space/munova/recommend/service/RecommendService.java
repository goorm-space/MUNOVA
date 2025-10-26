package com.space.munova.recommend.service;

import com.space.munova.recommend.dto.RecommendReasonResponseDto;
import com.space.munova.recommend.dto.RecommendationsProductResponseDto;
import com.space.munova.recommend.dto.ResponseDto;
import java.util.List;

public interface RecommendService {

    List<ResponseDto> getRecommendationsByUserId(Long userId);

    List<RecommendationsProductResponseDto> getRecommendationsByProductId(Long productId);

    void updateUserProductRecommend(Long userId, Long productId);
    void updateSimilarProductRecommend(Long productId);

    List<RecommendReasonResponseDto> getRecommendationReason(Long userId, Long productId);
    double getRecommendationScore(Long userId, Long productId);
    void updateUserAction(Long userId, Long productId, boolean clicked, boolean liked,
                          boolean inCart, boolean purchased);
}