package com.space.munova.product.ui;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.product.application.ProductService;
import com.space.munova.product.application.dto.AddProductRequestDto;
import com.space.munova.product.application.dto.ProductCategoryResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "상품", description = "상품 관련 API")
class ProductController {

    private final ProductService productService;

        /// 상품 등록 메서드
        @Operation(summary = "상품 세부사항 등록", description = "상품의 세부사항을 받아 상품을 등록한다. (판매자만 등록 가능)")
        @PostMapping(value = "/api/seller/product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ResponseApi<Void>> saveProduct (@RequestPart(name = "mainImgFile") MultipartFile mainImgFile,
                                                             @RequestPart(name = "sideImgFile") List<MultipartFile> sideImgFile,
                                                             @RequestPart(name = "addProductInforms") @Valid AddProductRequestDto reqDto) throws IOException {
            Long userId = 1L;
            Long brandId = 1L;
            productService.saveProduct(mainImgFile, sideImgFile, userId, brandId, reqDto);
            return ResponseEntity.ok().body(ResponseApi.ok());
        }

    /// 상품 등록 페이지 조회
    @Operation(summary = "상품 등록 페이지 조회", description = "상품의 카테고리를 바디에 담아 보내준다. (판매자만 조회 가능)")
    @GetMapping("/api/seller/product/new")
    public ResponseEntity<ResponseApi<List<ProductCategoryResponseDto>>> registProductView (){

        List<ProductCategoryResponseDto> productCategories = productService.findProductCategories();
        return ResponseEntity.ok().body(ResponseApi.ok(productCategories));
    }

}
