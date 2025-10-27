package com.space.munova.recommend.controller;

import com.space.munova.recommend.dto.RecommendationsProductResponseDto;
import com.space.munova.recommend.dto.RecommendationsUserResponseDto;
import com.space.munova.recommend.dto.ResponseDto;
import com.space.munova.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/recommend/users")
@RequiredArgsConstructor
public class RecommendUserController {

    private final RecommendService recommendService;

    //전체 회원 기반 추천 로그
    @GetMapping()
    public List<RecommendationsUserResponseDto> getAllMemberRecommendations() {
        return recommendService.getRecommendationsByMemberId(null);
    }
    //{MemberId}의 상품 기반 추천 로그
    @GetMapping("/{userId}")
    public List<RecommendationsUserResponseDto> getMemberRecommendations(@PathVariable Long memberId) {
        return recommendService.getRecommendationsByMemberId(memberId);
    }
}