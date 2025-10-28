package com.space.munova.product.ui;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.product.application.ProductLikeService;
import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.application.dto.like.ProductLikeRequestDto;
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

    @PostMapping("/api/like")
    public ResponseEntity<ResponseApi<Void>> productLike(@RequestBody ProductLikeRequestDto reqDto) {

        productLikeService.addLike(reqDto.productId());
        return  ResponseEntity.ok().body(ResponseApi.ok());
    }


    @DeleteMapping("/api/like")
    public ResponseEntity<ResponseApi<Void>> deleteProductLike(@RequestParam List<Long> productId) {

        productLikeService.deleteProductLikeByProductId(productId);
        return ResponseEntity.ok().body(ResponseApi.ok());
    }

    @GetMapping("/api/like")
    public ResponseEntity<ResponseApi<List<FindProductResponseDto>>> findProductLike(@PageableDefault Pageable pageable) {
        List<FindProductResponseDto> respDtos = productLikeService.findLikeProducts(pageable);
        return ResponseEntity.ok().body(ResponseApi.ok(respDtos));
    }

}
