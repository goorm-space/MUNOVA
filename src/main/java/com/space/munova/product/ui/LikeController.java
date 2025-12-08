package com.space.munova.product.ui;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.product.application.product.query.dto.FindProductResponseDto;
import com.space.munova.product.application.like.command.ProductLikeCommandService;
import com.space.munova.product.application.like.command.dto.ProductLikeRequestDto;
import com.space.munova.product.application.like.query.ProductLikeQueryService;
import com.space.munova.security.jwt.JwtHelper;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LikeController {

    private final ProductLikeQueryService productLikeQueryService;
    private final ProductLikeCommandService productLikeCommandService;


    @PostMapping("/api/like")
    public ResponseEntity<ResponseApi<Void>> productLike(@RequestBody ProductLikeRequestDto reqDto) {
        Long memberId = JwtHelper.getMemberId();
        productLikeCommandService.addLike(reqDto.productId(), memberId);
        return  ResponseEntity.ok().body(ResponseApi.ok());
    }


    @DeleteMapping("/api/like/{productId}")
    public ResponseEntity<ResponseApi<Void>> deleteProductLike(@PathVariable(name = "productId") @NotNull Long productId) {

        Long memberId = JwtHelper.getMemberId();
        productLikeCommandService.deleteProductLikeByProductId(productId ,memberId);

        return ResponseEntity.ok().body(ResponseApi.ok());
    }

    @GetMapping("/api/like")
    public ResponseEntity<ResponseApi<PagingResponse<FindProductResponseDto>>> findProductLike(@PageableDefault Pageable pageable) {
        Long memberId = JwtHelper.getMemberId();
        PagingResponse<FindProductResponseDto> likeProducts = productLikeQueryService.findLikeProducts(pageable, memberId);
        return ResponseEntity.ok().body(ResponseApi.ok(likeProducts));
    }

}
