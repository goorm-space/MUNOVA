package com.space.munova.product.ui;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.product.application.product.command.ProductCommandFacadeService;
import com.space.munova.product.application.product.command.dto.AddProductRequestDto;
import com.space.munova.product.application.product.command.dto.UpdateProductRequestDto;
import com.space.munova.product.application.product.query.ProductQueryFacadeService;
import com.space.munova.product.application.product.query.dto.*;
import com.space.munova.security.jwt.JwtHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    private final ProductCommandFacadeService productCommandFacadeService;
    private final ProductQueryFacadeService productQueryFacadeService;

    @PatchMapping(value = "/api/seller/product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseApi<Void>> updateProduct(@RequestPart(name = "mainImgFile", required = false) MultipartFile mainImgFile,
                                                           @RequestPart(name = "sideImgFile", required = false) List<MultipartFile> sideImgFile,
                                                           @RequestPart(name = "updateProductInfo") @Valid UpdateProductRequestDto reqDto) throws IOException {
        Long memberId = JwtHelper.getMemberId();
        productCommandFacadeService.updateProductInfo(mainImgFile, sideImgFile, reqDto, memberId);
        return ResponseEntity.ok().body(ResponseApi.ok());
    }


    /// 판매자의 수정 페이지 조회
    @GetMapping("/api/seller/product/{productId}/edit")
    public ResponseEntity<ResponseApi<ProductDetailResponseDto>> editProductView(@PathVariable("productId") Long productId) {

        Long memberId = JwtHelper.getMemberId();
        ProductDetailResponseDto respDto = productQueryFacadeService.findProductDetailsBySeller(productId, memberId);
        return ResponseEntity.ok().body(ResponseApi.ok(respDto));
    }

    /// 상품등록 페이지 카테고리, 옵션 조회 메서드
    @GetMapping("/api/seller/product/create")
    public ResponseEntity<ResponseApi<CreateProductConditionsResponseDto>> createProductView() {

        CreateProductConditionsResponseDto respDto = productQueryFacadeService.findCreateProductConditions();

        return ResponseEntity.ok().body(ResponseApi.ok(respDto));
    }


    ///  판매자의 등록상품 리스트 조회
    @GetMapping("/api/seller/product")
    public ResponseEntity<PagingResponse<FindProductResponseDto>> findProductBySeller(@PageableDefault Pageable pageable) {

        Long memberId = JwtHelper.getMemberId();
        PagingResponse<FindProductResponseDto> respDto = productQueryFacadeService.findProductBySeller(pageable, memberId);

        return ResponseEntity.ok().body(respDto);
    }

    /// 상품 등록 메서드
    @Operation(summary = "상품 세부사항 등록", description = "상품의 세부사항을 받아 상품을 등록한다. (판매자만 등록 가능)")
    @PostMapping(value = "/api/seller/product", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseApi<Void>> saveProduct(@RequestPart(name = "mainImgFile") @Valid @NotNull MultipartFile mainImgFile,
                                                         @RequestPart(name = "sideImgFile", required = false) List<MultipartFile> sideImgFile,
                                                         @RequestPart(name = "addProductInforms") @Valid AddProductRequestDto reqDto) throws IOException {

        Long memberId = JwtHelper.getMemberId();
        productCommandFacadeService.saveProduct(mainImgFile, sideImgFile, reqDto, memberId);
        return ResponseEntity.ok().body(ResponseApi.ok());
    }


    /// 상품목록 옵션
    @GetMapping("/product/options")
    public ResponseEntity<ResponseApi<List<ProductOptionResponseDto>>> findOptions() {

        List<ProductOptionResponseDto> resp = productQueryFacadeService.findOptions();

        return ResponseEntity.ok().body(ResponseApi.ok(resp));
    }

    /// 상품카테고리 조회
    @Operation(summary = "상품 카테고리 조회", description = "상품의 카테고리를 바디에 담아 보내준다. 모든 사용자")
    @GetMapping("/product/category")
    public ResponseEntity<ResponseApi<List<ProductCategoryResponseDto>>> registProductView() {

        List<ProductCategoryResponseDto> productCategories = productQueryFacadeService.findProductCategories();

        return ResponseEntity.ok().body(ResponseApi.ok(productCategories));
    }


    /// 상품 로그 + 조회 (로그인 한 경우)
    @GetMapping("/api/product")
    @Operation(summary = "상품 조회", description = "조건에 맞는 상품 조회")
    public ResponseEntity<ResponseApi<PagingResponse<FindProductResponseDto>>> findProductLogin(
            @ModelAttribute ProductSearchRequestDto productSearchRequestDto,
            @PageableDefault(size = 20) Pageable pageable) {
        PagingResponse<FindProductResponseDto> respDto =
                productQueryFacadeService.findProducts(productSearchRequestDto, pageable);

        return ResponseEntity.ok().body(ResponseApi.ok(respDto));
    }

    /// 상품조회
    @GetMapping("/product")
    @Operation(summary = "상품 조회", description = "조건에 맞는 상품 조회")
    public ResponseEntity<ResponseApi<PagingResponse<FindProductResponseDto>>> findProduct(
            @ModelAttribute ProductSearchRequestDto productSearchRequestDto,
            @PageableDefault(size = 20) Pageable pageable) {

        PagingResponse<FindProductResponseDto> respDto =
                productQueryFacadeService.findProducts(productSearchRequestDto, pageable);
        return ResponseEntity.ok().body(ResponseApi.ok(respDto));
    }

    /// (로그인)상품상세 조회
    @GetMapping("/api/product/{productId}")
    @Operation(summary = "상품상세 조회", description = "상품상세조회")
    public ResponseEntity<ResponseApi<ProductDetailResponseDto>> findProductDetailLogin(@PathVariable(name = "productId") Long productId) {

        ProductDetailResponseDto respDto = productQueryFacadeService.findProductDetails(productId);
        /// 상품 상세조회 시 로그
        return ResponseEntity.ok().body(ResponseApi.ok(respDto));
    }

    /// 상품상세 조회
    @GetMapping("/product/{productId}")
    @Operation(summary = "상품상세 조회", description = "상품상세조회")
    public ResponseEntity<ResponseApi<ProductDetailResponseDto>> findProductDetail(@PathVariable(name = "productId") Long productId) {
        ProductDetailResponseDto respDto = productQueryFacadeService.findProductDetails(productId);

        return ResponseEntity.ok().body(ResponseApi.ok(respDto));
    }


    @PatchMapping("/product/view/{productId}")
    @Operation(summary = "상품 조회수 업데이트", description = "상품조회 업데이트")
    public ResponseEntity<ResponseApi<Void>> updateViewCount(@PathVariable(name = "productId") Long productId) {

        /// 조회수 카운트 증가.
        productCommandFacadeService.updateProductViewCountLogin(productId);
        return ResponseEntity.ok().body(ResponseApi.ok());
    }


    /// 상품제거
    @DeleteMapping("/api/seller/product")
    @Operation(summary = "상품 삭제", description = "단건, 복수건 상품 삭제")
    public ResponseEntity<ResponseApi<Void>> deleteProduct(@RequestParam("productIds") List<Long> productIds) {
        Long memberId = JwtHelper.getMemberId();
        productCommandFacadeService.deleteProduct(productIds, memberId);
        return ResponseEntity.ok().body(ResponseApi.ok());
    }


    @GetMapping("/product/{productId}/options")
    public ResponseEntity<ResponseApi<List<ProductDetailInfoDto>>> findCartItemOption(@PathVariable(value = "productId") Long productId) {

        List<ProductDetailInfoDto> respDto = productQueryFacadeService.findProductOptionsByProductId(productId);
        return ResponseEntity.ok().body(ResponseApi.ok(respDto));
    }

    /// 상품상세페이지 좋아요수조회.
    @GetMapping("/product/like/{productId}")
    public ResponseEntity<ResponseApi<Integer>> findProductLikeCount(@PathVariable(value = "productId") Long productId) {

        Integer likeCount = productQueryFacadeService.findProductLikeCount(productId);
        return ResponseEntity.ok().body(ResponseApi.ok(likeCount));
    }

    /// 상품상세페이지 조회수 조회
    @GetMapping("/product/view/{productId}")
    public ResponseEntity<ResponseApi<Integer>> findProductViewCount(@PathVariable(value = "productId") Long productId) {

        Integer viewCount = productQueryFacadeService.findProductViewCount(productId);
        return ResponseEntity.ok().body(ResponseApi.ok(viewCount));
    }

}
