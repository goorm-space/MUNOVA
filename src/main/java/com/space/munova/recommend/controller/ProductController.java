package com.space.munova.recommend.controller;

import com.space.munova.recommend.dto.ResponseDto;
import com.space.munova.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommend/products")
@RequiredArgsConstructor
public class ProductController {

    private final RecommendService recommendService;

    @GetMapping("/{productId}")
    public List<ResponseDto> getProductRecommendations(@PathVariable Long productId) {
        return recommendService.getRecommendationsByProductId(productId);
    }

    @PostMapping
    public String createProductRecommendLog() {
        recommendService.createProductRecommendLog();
        return "Product recommend log created successfully";
    }

    @PutMapping("/{productId}")
    public String updateProductRecommendLog(@PathVariable Long productId) {
        recommendService.updateProductRecommendLog(productId);
        return "Product recommend log updated successfully";
    }
}