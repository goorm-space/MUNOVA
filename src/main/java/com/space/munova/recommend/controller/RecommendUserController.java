package com.space.munova.recommend.controller;

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

    @GetMapping("/{userId}")
    public List<ResponseDto> getUserRecommendations(@PathVariable Long userId) {
        return recommendService.getRecommendationsByUserId(userId);
    }
}