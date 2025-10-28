package com.space.munova.product.application;


import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.application.dto.*;
import com.space.munova.product.application.exception.ProductException;
import com.space.munova.product.domain.*;
import com.space.munova.product.domain.Repository.ProductClickLogRepository;
import com.space.munova.product.domain.Repository.ProductRepository;
import com.space.munova.product.domain.Repository.ProductSearchLogRepository;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;




@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductClickLogRepository productClickLogRepository;
    private final ProductRepository productRepository;
    private final ProductImageService productImageService;
    private final ProductDetailService productDetailService;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final MemberRepository memberRepository;
    //private final ProductLikeService productLikeService;  -> 이후 카프카로 이벤트를 쏴주어야함.
    private final ProductSearchLogRepository productSearchLogRepository;



    /// 모든 카테고리 조회 메서드
    public List<ProductCategoryResponseDto> findProductCategories() {
        return categoryService.findAllProductCategories();
    }

    /// 상품 등록 메서드
    @Transactional
    public void saveProduct(MultipartFile mainImgFile, List<MultipartFile> sideImgFile, AddProductRequestDto reqDto)  {

        ///멤버서비스에서 member객체를가져온다.
        Long sellerId = JwtHelper.getMemberId();

        Member seller = memberRepository.findById(sellerId).orElseThrow(MemberException::notFoundException);

        // 브랜드 조회.
        Brand brand = brandService.findById(reqDto.brandId());

        //카테고리 조회.
        Category category = categoryService.findById(reqDto.categoryId());

        // 상품생성
        try {
            Product product = Product.createDefaultProduct(reqDto.ProductName(),
                    reqDto.info(),
                    reqDto.price(),
                    brand,
                    category,
                    seller);
            Product savedProduct = productRepository.save(product);

            // 이미지 저장.
            productImageService.saveMainImg(mainImgFile, savedProduct);
            productImageService.saveSideImg(sideImgFile, savedProduct);

            // 상품 디테일 옵션 저장.
            productDetailService.saveProductDetailAndOption(savedProduct, reqDto.shoeOptionDtos());
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            throw ProductException.badRequestException(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException();
        }
    }

    public ProductDetailResponseDto findProductDetails(Long productId) {

        ProductInfoDto productInfoDto = productRepository.findProductInfoById(productId).orElseThrow(() -> ProductException.notFoundProductException("Not found Product Information By productId"));
        List<ProductDetailInfoDto> productDetailInfoDtoByProductId = productDetailService.findProductDetailInfoDtoByProductId(productId);
        ProductImageDto productImageDto = productImageService.findProductImageDtoByProductId(productId);
        ProductDetailResponseDto productDetailResponseDto = new ProductDetailResponseDto(productInfoDto, productImageDto, productDetailInfoDtoByProductId);
        return productDetailResponseDto;
    }

    @Transactional(readOnly = false)
    public void updateProductViewCount(Long productId) {
        productRepository.updateProductViewCount(productId);
    }

    @Transactional(readOnly = false)
    public void saveProductClickLog(Long productId) {
        Long memberId = JwtHelper.getMemberId();
        ProductClickLog log = ProductClickLog.builder()
                .memberId(memberId)
                .productId(productId)
                .build();

        productClickLogRepository.save(log);
    }

    /*
    * 상품 제거 메서드 (관련 테이블 모두 논리삭제) - 상품, 상품좋아요, 상품디테일, 상품이미지, 장바구니, 상품옵션매핑
    * */
    ///  현재 프로덕트를 삭제할때 카트와 좋아요를 한트랜잭션에 묶고 있지만 이후에 트랜잭션을 분리해야함.
    ///  상품 , 좋아요, 장바구니는 각각 어그리거트 루트가 다르다.
    @Transactional(readOnly = false)
    public void deleteProduct(List<Long> productIds) {

        Long sellerId = JwtHelper.getMemberId();

        productRepository.findAllById(productIds).forEach(product -> {

            if(!product.getMember().getId().equals(sellerId)) {
                throw ProductException.unauthorizedAccessException();
            }
        });

        productImageService.deleteImagesByProductIds(productIds);
        productDetailService.deleteProductDetailByProductId(productIds);
       // productLikeService.deleteProductLikeByProductId(productIds);
        productRepository.deleteAllByProductIds(productIds);

    }


    public List<FindProductResponseDto> findProductsWithOptionalLogging(Long categoryId, String keyword, List<Long> optionIds, Pageable pageable) {
        List<FindProductResponseDto> productByConditions = productRepository.findProductByConditions(categoryId,optionIds, keyword, pageable);
        return productByConditions.stream()
                .map(dto -> new FindProductResponseDto(
                        dto.productId(),
                        productImageService.getImgPath(dto.mainImgSrc()), ///  이미지 풀 패스로 변환.
                        dto.brandName(),
                        dto.productName(),
                        dto.price(),
                        dto.likeCount(),
                        dto.salesCount(),
                        dto.createAt())
                )
                .toList();
    }

    @Transactional
    public void saveSearchLog(Long categoryId, String keyword) {


        Long memberId = JwtHelper.getMemberId();

        ProductSearchLog log = ProductSearchLog.builder()
                .memberId(memberId)
                .searchDetail(keyword != null ? keyword : "")
                .searchCategoryId(categoryId)
                .build();
        productSearchLogRepository.save(log);

    }

    // 상품옵션 조회
    public List<ProductDetailInfoDto> findProductOptionsByProductId(Long productId) {
        return productDetailService.findProductDetailInfoDtoByProductId(productId);
    }


    public Product findByIdAndIsDeletedFalse(Long productId) {
        return productRepository.findByIdAndIsDeletedFalse(productId).orElseThrow(()-> ProductException.badRequestException("해당 상품 정보를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = false)
    public void minusLikeCountInProductIds(List<Long> productIds) {
        productRepository.minusLikeCountInProductIds(productIds);
    }
}
