package com.space.munova.product.application;


import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.application.dto.AddProductRequestDto;
import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.application.dto.ProductCategoryResponseDto;
import com.space.munova.product.domain.Brand;
import com.space.munova.product.domain.Category;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.ProductSearchLog;
import com.space.munova.product.application.dto.*;
import com.space.munova.product.application.exception.ProductException;
import com.space.munova.product.domain.*;
import com.space.munova.product.domain.Repository.ProductRepository;
import com.space.munova.product.domain.Repository.ProductSearchLogRepository;
import com.space.munova.security.jwt.JwtHelper;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageService productImageService;
    private final ProductDetailService productDetailService;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final MemberRepository memberRepository;
    private final ProductSearchLogRepository productSearchLogRepository;
    private final JwtHelper jwtHelper;
    private final SearchLogService searchLogService;


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

    private List<FindProductResponseDto> findProducts(Long categoryId, String keyword, List<Long> optionIds, Pageable pageable) {
        return productRepository.findProductByConditions(categoryId,optionIds, keyword, pageable);
    }

    //통합된 상품 조회
    public List<FindProductResponseDto> findProductsWithOptionalLogging(Long categoryId, String keyword, List<Long> optionIds, Pageable pageable, boolean doLogging) {
        List<FindProductResponseDto> productByConditions=findProducts(categoryId,keyword,optionIds,pageable);
        if(doLogging && (categoryId!=null || (keyword!=null && !keyword.isEmpty()) || (optionIds!=null && !optionIds.isEmpty()))) {
            searchLogService.saveSearchLog(categoryId,keyword);
        }
        return productByConditions;
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
}
