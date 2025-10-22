package com.space.munova.recommend.controller;

import com.space.munova.recommend.dto.ResponseDto;
import com.space.munova.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/recommend/users")
@RequiredArgsConstructor
public class UserController {

    private final RecommendService recommendService;

    @GetMapping("/{userId}")
    public List<ResponseDto> getUserRecommendations(@PathVariable Long userId) {
        return recommendService.getRecommendationsByUserId(userId);
    }

    @PostMapping("/{userId}")
    public String createUserRecommendLog(@PathVariable Long userId) {
        recommendService.createUserRecommendLog(userId);
        return "User recommendations log created successfully";
    }

    @PutMapping("/{userId}")
    public String updateUserRecommendLog(@PathVariable Long userId) {
        recommendService.updateUserRecommendLog(userId);
        return "User recommendations log updated successfully";
    }
}