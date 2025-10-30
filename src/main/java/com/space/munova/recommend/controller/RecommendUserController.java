package com.space.munova.recommend.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.recommend.dto.RecommendationsUserResponseDto;
import com.space.munova.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/recommend/users")
@RequiredArgsConstructor
public class RecommendUserController {

    private final RecommendService recommendService;

    //전체 회원 기반 추천 로그
    @GetMapping()
    public ResponseEntity<ResponseApi<PagingResponse<RecommendationsUserResponseDto>>> getAllMemberRecommendations(@PageableDefault(size = 10, sort="CreatedAt") Pageable pageable) {
        PagingResponse<RecommendationsUserResponseDto> recommendations = recommendService.getRecommendationsByMemberId(null,pageable);
        return ResponseEntity.ok().body(ResponseApi.ok(recommendations));
    }
    //{MemberId}의 상품 기반 추천 로그
    @GetMapping("/{userId}")
    public ResponseEntity<ResponseApi<PagingResponse<RecommendationsUserResponseDto>>> getMemberRecommendations(@PathVariable Long memberId,@PageableDefault(size = 10, sort="CreatedAt") Pageable pageable) {
        PagingResponse<RecommendationsUserResponseDto> recommendations = recommendService.getRecommendationsByMemberId(memberId,pageable);
        return ResponseEntity.ok().body(ResponseApi.ok(recommendations));
    }
}