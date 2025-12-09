package com.space.munova.product.application.product.query;


import com.space.munova.core.dto.PagingResponse;
import com.space.munova.product.application.product.command.port.ProductRedisCommandPort;
import com.space.munova.product.application.product.query.dto.*;
import com.space.munova.product.application.product.query.exception.ProductQueryException;
import com.space.munova.product.application.product.query.port.ProductDetailsPort;
import com.space.munova.product.application.product.query.port.ProductListPort;
import com.space.munova.product.application.product.query.port.ProductRedisQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/// 읽기전용 퍼사드 서비스
/// 몽고, 엘라스틱, 레디스등 인프라 레이어를 포트 어뎁터 패턴으로 분리.
@Service
@RequiredArgsConstructor
public class ProductQueryFacadeService {

    /// 포트 의존성주입
    private final List<ProductListPort>  productSearchPorts;
    private final ProductDetailsPort productDetailsPort;
    private final ProductRedisQueryPort productRedisQueryPort;
    /// 레포 의존성 주입.
    private final ProductQueryOptionService productQueryOptionService;
    private final ProductQueryCategoryService productQueryCategoryService;
    private final ProductQueryDetailService productQueryDetailService;
    private final ProductQueryService productQueryService;
    private final ProductQueryImageService productQueryImageService;




    ///  키워드 유무 상품목록조회.
    ///  키워드 있을경우 -> ES
    ///  키워드 없을경우 -> MONGO
    public PagingResponse<FindProductResponseDto> findProducts(ProductSearchRequestDto productSearchRequestDto, Pageable pageable) {

        ProductListPort selectedPort = productSearchPorts.stream()
                .filter(p -> p.supports(productSearchRequestDto))
                .findFirst()
                .orElseThrow(() -> ProductQueryException.badRequestException("잘못된 요청입니다."));

        return PagingResponse.from(selectedPort.findList(productSearchRequestDto, pageable));
    }


    /// 상품 상세조회. -> MONGO
    public ProductDetailResponseDto findProductDetails(Long productId) {
        return productDetailsPort.findProductDetails(productId);
    }


    /// 상품옵션 조회 - mysql 조회
    public List<ProductDetailInfoDto> findProductOptionsByProductId(Long productId) {
        return productQueryDetailService.findProductDetailInfoDtoByProductId(productId);
    }

    /// 상품옵션조회. - mysql 조회
    public List<ProductOptionResponseDto> findOptions() {
        return productQueryOptionService.findOptions();
    }

    /// 상품 카테고리 조회 - 서버 이넘 캐싱조회
    public List<ProductCategoryResponseDto> findProductCategories() {
        return productQueryCategoryService.findProductCategories();
    }

    ///  상품등록시 옵션, 카테고리 조회메서드 - mysql , 서버 이넘 캐싱조회
    public CreateProductConditionsResponseDto findCreateProductConditions() {
        return new CreateProductConditionsResponseDto(
                findOptions(),
                findProductCategories()
        );
    }

    /// 판매자 등록상품리스트 조회 - mysql 조회
    public PagingResponse<FindProductResponseDto> findProductBySeller(Pageable pageable, Long memberId) {
        return productQueryService.findProductBySeller(pageable, memberId);
    }


    /// 판매자 등록상품 상세 조회 (수정페이지) - mysql조회
    public ProductDetailResponseDto findProductDetailsBySeller(Long productId, Long sellerId) {

        ProductInfoDto productInfoDto = productQueryService.findProductByIdAndSellerId(productId, sellerId);
        ProductImageDto productImageDto = productQueryImageService.findProductImageDtoByProductId(productId);
        List<ProductDetailInfoDto> productDetailInfoDtoByProductId = productQueryDetailService.findProductDetailInfoDtoByProductId(productId);

        return new ProductDetailResponseDto(productInfoDto, productImageDto, productDetailInfoDtoByProductId);
    }

    /// 상품 좋아요수 조회.
    public Integer findProductLikeCount(Long productId) {

        return productRedisQueryPort.findProductLikeCount(productId);
    }

    /// 상품 조회수 조회
    public Integer findProductViewCount(Long productId) {

        return productRedisQueryPort.findProductViewCount(productId);
    }
}
