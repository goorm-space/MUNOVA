package com.space.munova.product.ui;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.product.application.ProductLikeService;
import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.application.dto.like.ProductLikeRequestDto;
import com.space.munova.recommend.service.RecommendService;
import com.space.munova.security.jwt.JwtHelper;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LikeController {

    private final ProductLikeService productLikeService;
    private final RecommendService recommendService;

    @PostMapping("/api/like")
    public ResponseEntity<ResponseApi<Void>> productLike(@RequestBody ProductLikeRequestDto reqDto) {
        Long memberId = JwtHelper.getMemberId();
        productLikeService.addLike(reqDto.productId(), memberId);
        return  ResponseEntity.ok().body(ResponseApi.ok());
    }


    @DeleteMapping("/api/like/{productId}")
    public ResponseEntity<ResponseApi<Void>> deleteProductLike(@PathVariable(name = "productId") @NotNull Long productId) {

        Long memberId = JwtHelper.getMemberId();
        productLikeService.deleteProductLikeByProductId(productId ,memberId);

        return ResponseEntity.ok().body(ResponseApi.ok());
    }

    @GetMapping("/api/like")
    public ResponseEntity<ResponseApi<PagingResponse<FindProductResponseDto>>> findProductLike(@PageableDefault Pageable pageable) {
        Long memberId = JwtHelper.getMemberId();
        PagingResponse<FindProductResponseDto> likeProducts = productLikeService.findLikeProducts(pageable, memberId);
        return ResponseEntity.ok().body(ResponseApi.ok(likeProducts));
    }

}
