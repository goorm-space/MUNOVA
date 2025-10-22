package com.space.munova.recommend.controller;

import com.space.munova.recommend.dto.ResponseDto;
import com.space.munova.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommend")
@RequiredArgsConstructor
public class CommonController {

    private final RecommendService recommendService;

    @PutMapping("/{userId}/{productId}")
    public String updateMemberProductRecommend(@PathVariable Long userId, @PathVariable Long productId) {
        recommendService.updateUserProductRecommend(userId, productId);
        return "User recommendation updated successfully";
    }

    @PutMapping("/{productId}")
    public String updateSimilarProductRecommend(@PathVariable Long productId) {
        recommendService.updateSimilarProductRecommend(productId);
        return "Similar product recommendation updated successfully";
    }

    @GetMapping("/user/{userId}/product/{productId}/based_on")
    public List<ResponseDto> getRecommendationReason(@PathVariable Long userId, @PathVariable Long productId) {
        return recommendService.getRecommendationReason(userId, productId);
    }

    @GetMapping("/user/{userId}/product/{productId}/score")
    public double getRecommendationScore(@PathVariable Long userId, @PathVariable Long productId) {
        return recommendService.getRecommendationScore(userId, productId);
    }
}