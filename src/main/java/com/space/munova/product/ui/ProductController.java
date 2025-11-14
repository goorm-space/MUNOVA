package com.space.munova.product.ui;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.coupon.dto.SearchCouponResponse;
import com.space.munova.product.application.ProductService;
import com.space.munova.product.application.dto.*;
import com.space.munova.recommend.service.RecommendService;
import com.space.munova.security.jwt.JwtHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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

    @PatchMapping(value = "/api/seller/product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseApi<Void>> updateProduct(@RequestPart(name = "mainImgFile", required = false) MultipartFile mainImgFile,
                                                           @RequestPart(name = "sideImgFile", required = false) List<MultipartFile> sideImgFile,
                                                           @RequestPart(name = "updateProductInfo") @Valid UpdateProductRequestDto reqDto) throws IOException {
        Long memberId = JwtHelper.getMemberId();
        productService.updateProductInfo(mainImgFile, sideImgFile, reqDto, memberId);
        return ResponseEntity.ok().body(ResponseApi.ok());
    }


    /// 판매자의 수정 페이지 조회
    @GetMapping("/api/seller/product/{productId}/edit")
    public ResponseEntity<ResponseApi<ProductDetailResponseDto>> editProductView(@PathVariable("productId") Long productId){

        Long memberId = JwtHelper.getMemberId();
        ProductDetailResponseDto respDto = productService.findProductDetailsBySeller(productId, memberId);
        return ResponseEntity.ok().body(ResponseApi.ok(respDto));
    }

    @GetMapping("/api/seller/product/create")
    public ResponseEntity<ResponseApi<CreateProductConditionsResponseDto>> createProductView(){

        CreateProductConditionsResponseDto respDto =  productService.findCreateProductConditions();

        return ResponseEntity.ok().body(ResponseApi.ok(respDto));
    }



    ///  판매자의 등록상품 리스트 조회
    @GetMapping("/api/seller/product")
    public ResponseEntity<PagingResponse<FindProductResponseDto>> findProductBySeller(@PageableDefault Pageable pageable) {

        Long memberId = JwtHelper.getMemberId();
        PagingResponse<FindProductResponseDto> respDto = productService.findProductBySeller(pageable, memberId);
        return ResponseEntity.ok().body(respDto);
    }

    /// 상품 등록 메서드
    @Operation(summary = "상품 세부사항 등록", description = "상품의 세부사항을 받아 상품을 등록한다. (판매자만 등록 가능)")
    @PostMapping(value = "/api/seller/product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseApi<Void>> saveProduct(@RequestPart(name = "mainImgFile") @Valid @NotNull MultipartFile mainImgFile,
                                                         @RequestPart(name = "sideImgFile", required = false) List<MultipartFile> sideImgFile,
                                                         @RequestPart(name = "addProductInforms") @Valid AddProductRequestDto reqDto) throws IOException {

        Long memberId = JwtHelper.getMemberId();
        productService.saveProduct(mainImgFile, sideImgFile, reqDto, memberId);
        return ResponseEntity.ok().body(ResponseApi.ok());
    }

    @GetMapping("/product/options")
    public ResponseEntity<ResponseApi<List<ProductOptionResponseDto>>> findOptions() {

        List<ProductOptionResponseDto> resp = productService.findOptions();

        return ResponseEntity.ok().body(ResponseApi.ok(resp));
    }

    /// 상품카테고리 조회
    @Operation(summary = "상품 카테고리 조회", description = "상품의 카테고리를 바디에 담아 보내준다. 모든 사용자")
    @GetMapping("/product/category")
    public ResponseEntity<ResponseApi<List<ProductCategoryResponseDto>>> registProductView() {

        List<ProductCategoryResponseDto> productCategories = productService.findProductCategories();

        return ResponseEntity.ok().body(ResponseApi.ok(productCategories));
    }



    /// 상품 로그 + 조회 (로그인 한 경우)
    @GetMapping("/api/product")
    @Operation(summary = "상품 조회", description = "조건에 맞는 상품 조회")
    public ResponseEntity<ResponseApi<PagingResponse<FindProductResponseDto>>> findProductLogin(@RequestParam(name = "categoryId", required = false) Long categoryId,
                                                                                      @RequestParam(name = "keyword", required = false) String keyword,
                                                                                      @RequestParam(name = "optionIds", required = false) List<Long> optionIds,
                                                                                      @PageableDefault Pageable pageable) {
        PagingResponse<FindProductResponseDto> respDto = productService.findProductsWithOptionalLogging(categoryId, keyword, optionIds, pageable);
        if ((categoryId != null || (keyword != null && !keyword.isEmpty()) || (optionIds != null && !optionIds.isEmpty()))) {
            productService.saveSearchLog(categoryId, keyword);
        }
        return ResponseEntity.ok().body(ResponseApi.ok(respDto));
    }

    /// 상품조회
    @GetMapping("/product")
    @Operation(summary = "상품 조회", description = "조건에 맞는 상품 조회")
    public ResponseEntity<ResponseApi<PagingResponse<FindProductResponseDto>>> findProduct(@RequestParam(name = "categoryId", required = false) Long categoryId,
                                                                                 @RequestParam(name = "keyword", required = false) String keyword,
                                                                                 @RequestParam(name = "optionIds", required = false) List<Long> optionIds,
                                                                                 @PageableDefault Pageable pageable) {
        PagingResponse<FindProductResponseDto> respDto = productService.findProductsWithOptionalLogging(categoryId, keyword, optionIds, pageable);
        return ResponseEntity.ok().body(ResponseApi.ok(respDto));
    }

    /// (로그인)상품상세 조회
    @GetMapping("/api/product/{productId}")
    @Operation(summary = "상품상세 조회", description = "상품상세조회")
    public ResponseEntity<ResponseApi<ProductDetailResponseDto>> findProductDetailLogin(@PathVariable(name = "productId") Long productId) {
        ProductDetailResponseDto respDto = productService.findProductDetails(productId);
        /// 상품 상세조회 시 로그
        productService.saveProductClickLog(productId);
        /// 조회수 카운트 증가.
        productService.updateProductViewCountLogin(productId);
        return ResponseEntity.ok().body(ResponseApi.ok(respDto));
    }

    /// 상품상세 조회
    @GetMapping("/product/{productId}")
    @Operation(summary = "상품상세 조회", description = "상품상세조회")
    public ResponseEntity<ResponseApi<ProductDetailResponseDto>> findProductDetail(@PathVariable(name = "productId") Long productId) {
        ProductDetailResponseDto respDto = productService.findProductDetails(productId);

        /// 조회수 카운트 증가.
        productService.updateProductViewCount(productId);
        return ResponseEntity.ok().body(ResponseApi.ok(respDto));
    }


    /// 상품제거
    @DeleteMapping("/api/seller/product")
    @Operation(summary = "상품 삭제", description = "단건, 복수건 상품 삭제")
    public ResponseEntity<ResponseApi<Void>> deleteProduct(@RequestParam("productIds") List<Long> productIds) {
        Long memberId = JwtHelper.getMemberId();
        productService.deleteProduct(productIds, memberId);
        return ResponseEntity.ok().body(ResponseApi.ok());
    }


    @GetMapping("/product/{productId}/options")
    public ResponseEntity<ResponseApi<List<ProductDetailInfoDto>>> findCartItemOption(@PathVariable(value = "productId") Long productId) {

        List<ProductDetailInfoDto> respDto = productService.findProductOptionsByProductId(productId);
        return ResponseEntity.ok().body(ResponseApi.ok(respDto));
    }
}
