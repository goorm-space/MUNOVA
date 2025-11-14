package com.space.munova.product.application;


import com.space.munova.core.dto.PagingResponse;
import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.application.dto.*;
import com.space.munova.product.application.event.ProductDeleteEvenForLikeDto;
import com.space.munova.product.application.event.ProductDeleteEventForCartDto;
import com.space.munova.product.application.exception.ProductException;
import com.space.munova.product.domain.*;
import com.space.munova.product.domain.Repository.ProductClickLogRepository;
import com.space.munova.product.domain.Repository.ProductRepository;
import com.space.munova.product.domain.Repository.ProductSearchLogRepository;
import com.space.munova.product.domain.enums.ProductCategory;
import com.space.munova.recommend.infra.RedisStreamProducer;
import com.space.munova.recommend.service.RecommendService;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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
    private final ProductOptionService productOptionService;
    private final ProductSearchLogRepository productSearchLogRepository;
    private final RecommendService recommendService;
    private final RedisStreamProducer logProducer;

    ///  스프린 내부 이벤트 발행 인터페이스 추가
    private final ApplicationEventPublisher eventPublisher;

    /// 모든 카테고리 조회 메서드
    public List<ProductCategoryResponseDto> findProductCategories() {
        return ProductCategory.findCategoryInfoList();
    }

    /// 상품 등록 메서드
    @Transactional
    public void saveProduct(MultipartFile mainImgFile, List<MultipartFile> sideImgFile, AddProductRequestDto reqDto, Long sellerId)  {

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
        return getProductDetailResponseDto(productId);
    }

    public ProductDetailResponseDto findProductDetailsBySeller(Long productId, Long sellerId) {

        /// 판매자가 등록한상품이 아닐경우 에러 터트림.
        if(!productRepository.existsByIdAndMemberIdAndIsDeletedFalse(productId, sellerId)){
            throw ProductException.badRequestException("등록한 상품을 찾을 수 없습니다.");
        }

        return getProductDetailResponseDto(productId);
    }


    @Transactional(readOnly = false)
    public void updateProductViewCount(Long productId) {
        productRepository.updateProductViewCount(productId);

    }
    @Transactional(readOnly = false)
    public void updateProductViewCountLogin(Long productId) {
        productRepository.updateProductViewCount(productId);
        recommendService.updateUserAction(productId, 1, null, null, null);
    }

    @Transactional(readOnly = false)
    public void saveProductClickLog(Long productId) {
        Long memberId = JwtHelper.getMemberId();
        Map<String, Object> logData = Map.of(
                "event_type", "product_detail_view",
                "service", "product",
                "member_id", memberId,
                "data", Map.of(
                        "product_id", productId
                )
        );
        logProducer.sendLogAsync(RedisStreamProducer.StreamType.PRODUCT, logData);

    }

    /*
    * 상품 제거 메서드 (관련 테이블 모두 논리삭제) - 상품, 상품좋아요, 상품디테일, 상품이미지, 장바구니, 상품옵션매핑
    * */

    ///  현재 프로덕트를 삭제할때 카트와 좋아요를 한트랜잭션에 묶고 있지만 이후에 트랜잭션을 분리해야함.
    ///  상품 , 좋아요, 장바구니는 각각 어그리거트 루트가 다르다.
    @Transactional(readOnly = false)
    public void deleteProduct(List<Long> productIds, Long sellerId) {


        List<Product> productBySeller = productRepository.findAllByIdAndMemberId(productIds, sellerId);

        List<Long> filteredProductIds = productBySeller.stream().map(Product::getId).toList();

        productImageService.deleteImagesByProductIds(filteredProductIds);

        /// 삭제된 디테일 아이디 값반환.
        List<Long> deletedDetailIds = productDetailService.deleteProductDetailByProductId(filteredProductIds);

        productRepository.deleteAllByProductIds(filteredProductIds);

        /// 비동기로 장바구니, 좋아요에 상품 삭제 메시지 발행
        ProductDeleteEventForCartDto deleteCartMessage = new ProductDeleteEventForCartDto(deletedDetailIds, true);
        ProductDeleteEvenForLikeDto deleteLikeMessage = new ProductDeleteEvenForLikeDto(filteredProductIds, true);
        /// 라이크 제거 메세지 발행
        eventPublisher.publishEvent(deleteLikeMessage);
        ///  장바구니 제거 메세지 발행
        eventPublisher.publishEvent(deleteCartMessage);
    }




    public PagingResponse<FindProductResponseDto> findProductsWithOptionalLogging(Long categoryId, String keyword, List<Long> optionIds, Pageable pageable) {
        Page<FindProductResponseDto> retVal = productRepository.findProductByConditions(categoryId,optionIds, keyword, pageable);

        return PagingResponse.from(retVal);
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
    public int minusLikeCountInProductIds(Long productId) {
        int rowCount = productRepository.minusLikeCountInProductIds(productId);
        if(rowCount == 0) {
            throw ProductException.notFoundProductException("취소한 상품을 찾을 수 없습니다.");
        }
        return rowCount;
    }


    public PagingResponse<FindProductResponseDto> findProductBySeller(Pageable pageable, Long sellerId) {


        Page<FindProductResponseDto> retVal = productRepository.findProductBySeller(pageable, sellerId);

        return PagingResponse.from(retVal);
    }

    @Transactional(readOnly = false)
    public void updateProductInfo(MultipartFile mainImgFile, List<MultipartFile> sideImgFile, UpdateProductRequestDto reqDto, Long sellerId) throws IOException {

        Product product = productRepository.findByIdAndMemberIdAndIsDeletedFalse(reqDto.productId(), sellerId)
                .orElseThrow(() -> ProductException.badRequestException("등록한 상품을 찾을 수 없습니다."));

        // 상품수정
        try {
            product.updateProduct(reqDto.ProductName(), reqDto.info(), reqDto.price());

            /// 이미지 수정
            /// 메인이미지가 넘어왔을경우 메인이미지 업데이트
            if(mainImgFile != null &&  !mainImgFile.isEmpty())  {
                productImageService.updateMainImg(mainImgFile, product);
            }

            /// 사이드 이미지가 넘어왔을경우 업데이틑
            if(sideImgFile != null &&  !sideImgFile.isEmpty())  {
                productImageService.saveSideImg(sideImgFile, product);
            }

            productImageService.deleteImagesByImgIds(reqDto.deletedImgIds(), product.getId());

            ProductDetailUpdateDtos productDetailUpdateDtos = ProductDetailUpdateDtos.from(reqDto);

            /// 삭제아이템과 업데이트아이템이 겹칠경우 업데이트아이템에서 삭제된아이템제거
            productDetailUpdateDtos.removeDeletedItemsFromUpdateList();

            if(!productDetailUpdateDtos.addShoeOptionDtos().isEmpty()) {
                productDetailService.saveProductDetailAndOption(product, reqDto.addShoeOptionDto().shoeOptionDtos());
            }

            if(!productDetailUpdateDtos.updateQuantityDtos().isEmpty()) {
                productDetailService.updateQuantity(productDetailUpdateDtos.updateQuantityDtos());
            }

            if(!productDetailUpdateDtos.deleteDetailIds().isEmpty()) {
                productDetailService.deleteProductDetailByIds(productDetailUpdateDtos.deleteDetailIds());
            }

        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            throw ProductException.badRequestException(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException();
        }

    }


    public List<ProductOptionResponseDto> findOptions() {
        return productOptionService.findOptions();
    }


    public CreateProductConditionsResponseDto findCreateProductConditions() {
        return new CreateProductConditionsResponseDto(
                findOptions(),
                findProductCategories()
        );
    }


    @Transactional(readOnly = false)
    public void plusLikeCountByProductId(Long productId) {
        productRepository.plusLikeCountByProductId(productId);
    }

    private ProductDetailResponseDto getProductDetailResponseDto(Long productId) {

        ProductInfoDto productInfoDto = productRepository.findProductInfoById(productId)
                .orElseThrow(() -> ProductException.notFoundProductException("상품 정보를 찾지 못했습니다."));

        ProductImageDto productImageDto = productImageService.findProductImageDtoByProductId(productId);

        List<ProductDetailInfoDto> productDetailInfoDtoByProductId = productDetailService.findProductDetailInfoDtoByProductId(productId);

        return new ProductDetailResponseDto(productInfoDto, productImageDto, productDetailInfoDtoByProductId);
    }

    private record ProductDetailUpdateDtos(List<ShoeOptionDto> addShoeOptionDtos
            , List<UpdateQuantityDto> updateQuantityDtos
            , List<Long> deleteDetailIds) {

        public static ProductDetailUpdateDtos from(UpdateProductRequestDto reqDto) {
            List<ShoeOptionDto> addShoeOptionDtos = reqDto.addShoeOptionDto() == null
                    ? new ArrayList<>()
                    : reqDto.addShoeOptionDto().shoeOptionDtos();
            List<UpdateQuantityDto> updateQuantityDtos = reqDto.updateQuantityDto() == null
                    ? new ArrayList<>()
                    : reqDto.updateQuantityDto();
            List<Long> deleteDetailIds = reqDto.deleteProductDetailDto() == null
                    ? new ArrayList<>()
                    : reqDto.deleteProductDetailDto().detailId();

            return new ProductDetailUpdateDtos(addShoeOptionDtos, updateQuantityDtos, deleteDetailIds);
        }

        public void removeDeletedItemsFromUpdateList() {
            if(this.updateQuantityDtos.isEmpty() || this.deleteDetailIds.isEmpty()) {
                return;
            }

            updateQuantityDtos.removeIf(dto -> deleteDetailIds.contains(dto.detailId()));
        }

    }

}
