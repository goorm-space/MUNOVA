package com.space.munova.product.application.product.command;

import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.application.product.command.event.*;
import com.space.munova.product.application.product.command.dto.AddProductRequestDto;
import com.space.munova.product.application.product.command.dto.ProductDetailUpdateDtos;
import com.space.munova.product.application.product.command.dto.SavedDetailAndOptionInfoDto;
import com.space.munova.product.application.product.command.dto.UpdateProductRequestDto;
import com.space.munova.product.application.product.command.port.OutboxCommandPort;
import com.space.munova.product.application.product.command.port.ProductRedisCommandPort;
import com.space.munova.product.domain.Brand;
import com.space.munova.product.domain.Category;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.ProductImage;
import com.space.munova.product.infra.elasticsearch.ProductEsDocument;
import com.space.munova.product.infra.mongo.ProductMongoDocument;
import com.space.munova.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = false)
public class ProductCommandFacadeService {

    private final ProductRedisCommandPort productRedisCommandPort;
    private final OutboxCommandPort outboxCommandPort;

    private final ProductImageCommandService productImageCommandService;
    private final ProductDetailService productDetailService;
    private final BrandCommandService brandCommandService;
    private final CategoryCommandService categoryCommandService;
    private final ProductCommandService productCommandService;
    private final MemberRepository memberRepository;
    private final RecommendService recommendService;



    public void updateProductViewCountLogin(Long productId) {
        productRedisCommandPort.updateViewCount(productId, 1);
        recommendService.updateUserAction(productId, 1, null, null, null);
    }

    public void saveProduct(MultipartFile mainImgFile,
                            List<MultipartFile> sideImgFile,
                            AddProductRequestDto reqDto,
                            Long sellerId) throws IOException {

        /// 판매자 조회
        Member seller = memberRepository.findById(sellerId).orElseThrow(MemberException::notFoundException);

        /// 브랜드 조회.
        Brand brand = brandCommandService.findById(reqDto.brandId());

        /// 카테고리 조회.
        Category category = categoryCommandService.findById(reqDto.categoryId());

        Product savedProduct = productCommandService.saveProduct(reqDto, seller, brand, category);

        /// 이미지 저장.
        ProductImage mainImg = productImageCommandService.saveMainImg(mainImgFile, savedProduct);
        List<ProductImage> sideImgs = productImageCommandService.saveSideImg(sideImgFile, savedProduct);

        /// 상품 디테일 옵션 저장.
        SavedDetailAndOptionInfoDto savedDetailAndOptionInfoDto = productDetailService.saveProductDetailAndOption(savedProduct, reqDto.shoeOptionDtos());

        /// 몽고상품문서 .
        ProductMongoDocument productMongoDocument = ProductMongoDocument.from(savedProduct,
                brand,
                category,
                mainImg,
                sideImgs,
                savedDetailAndOptionInfoDto);

        /// 엘라스틱 상품문서
        ProductEsDocument productEsDocument = ProductEsDocument.from(savedProduct,
                brand,
                category,
                mainImg,
                savedDetailAndOptionInfoDto);

        /// 아웃박스 테이블저장 todo
        outboxCommandPort.syncSaveEsEvent(productEsDocument);
        outboxCommandPort.syncSaveMongoEvent(productMongoDocument);
    }


    /*
     * 상품 제거 메서드 (관련 테이블 모두 논리삭제) - 상품, 상품좋아요, 상품디테일, 상품이미지, 장바구니, 상품옵션매핑
     * */
    public void deleteProduct(List<Long> productIds, Long sellerId) {

        List<Long> deleteProductIds = productCommandService.findDeleteProductIds(productIds, sellerId);

        /// 이미지 삭제
        productImageCommandService.deleteImagesByProductIds(deleteProductIds);

        /// 삭제된 디테일 아이디 값반환.
        List<Long> deletedDetailIds = productDetailService.deleteProductDetailByProductId(deleteProductIds);

        productCommandService.deleteProduct(deleteProductIds);

        /// 비동기로 장바구니, 좋아요에 상품 삭제 메시지 발행
        ProductDeleteEventForCartDto deleteCartMessage = new ProductDeleteEventForCartDto(deletedDetailIds, true);
        ProductDeleteEvenForLikeDto deleteLikeMessage = new ProductDeleteEvenForLikeDto(deleteProductIds, true);
        ProductDocDeleteEventDto deleteProductMessage = new ProductDocDeleteEventDto(deleteProductIds, true);

        /// 아웃박스 테이블저장 todo
        outboxCommandPort.deleteCartEvent(deleteCartMessage);
        outboxCommandPort.deleteLikeEvent(deleteLikeMessage);
        outboxCommandPort.syncDeleteEsEvent(deleteProductMessage);
        outboxCommandPort.syncDeleteMongoEvent(deleteProductMessage);
    }


    public void updateProductInfo(MultipartFile mainImgFile, List<MultipartFile> sideImgFile, UpdateProductRequestDto reqDto, Long sellerId) throws IOException {

        Product product = productCommandService.findProductBySeller(reqDto.productId(), sellerId);

        // 상품수정
        product.updateProduct(reqDto.ProductName(), reqDto.info(), reqDto.price());

        ProductImage updatedMainImg = null;
        SavedDetailAndOptionInfoDto savedDetailAndOptionInfoDto = null;
        List<ProductImage> addSideImages = new ArrayList<>();
        List<ProductImage> removeSideImages = new ArrayList<>();

        /// 이미지 수정
        /// 메인이미지가 넘어왔을경우 메인이미지 업데이트
        if(mainImgFile != null && !mainImgFile.isEmpty())  {
            updatedMainImg = productImageCommandService.updateMainImg(mainImgFile, product);
        }

        /// 사이드 이미지가 넘어왔을경우 업데이틑
        if(sideImgFile != null &&  !sideImgFile.isEmpty())  {
            List<ProductImage> productImages = productImageCommandService.saveSideImg(sideImgFile, product);
            addSideImages.addAll(productImages);
        }


        if(reqDto.deletedImgIds() != null || !reqDto.deletedImgIds().isEmpty()) {
            removeSideImages = productImageCommandService.deleteImagesByImgIds(reqDto.deletedImgIds(), product.getId());
        }


        ProductDetailUpdateDtos productDetailUpdateDtos = ProductDetailUpdateDtos.from(reqDto);

        /// 삭제아이템과 업데이트아이템이 겹칠경우 업데이트아이템에서 삭제된아이템제거
        productDetailUpdateDtos.removeDeletedItemsFromUpdateList();

        if(!productDetailUpdateDtos.addShoeOptionDtos().isEmpty()) {
             savedDetailAndOptionInfoDto = productDetailService.saveProductDetailAndOption(product,
                     productDetailUpdateDtos.addShoeOptionDtos());

        }

        if(!productDetailUpdateDtos.updateQuantityDtos().isEmpty()) {
            productDetailService.updateQuantity(productDetailUpdateDtos.updateQuantityDtos());
        }

        if(!productDetailUpdateDtos.deleteDetailIds().isEmpty()) {
            productDetailService.deleteProductDetailByIds(productDetailUpdateDtos.deleteDetailIds());
        }

        ///  todo - 아웃박스테이블 저장
        ProductImageEventDto mainImageDto = updatedMainImg == null ? null :
                new ProductImageEventDto(
                        updatedMainImg.getId(),
                        updatedMainImg.getImgUrl(),
                        updatedMainImg.getImageType(),
                        updatedMainImg.isDeleted()
                );

        List<ProductImageEventDto> addSideImageDtos = addSideImages.stream()
                .map(img -> new ProductImageEventDto(img.getId(), img.getImgUrl(), img.getImageType(), img.isDeleted()))
                .toList();

        List<ProductImageEventDto> removeSideImageDtos = removeSideImages.stream()
                .map(img -> new ProductImageEventDto(img.getId(), img.getImgUrl(), img.getImageType(), img.isDeleted()))
                .toList();

        ProductUpdateEventDto productUpdateEventDto = new ProductUpdateEventDto(reqDto.productId(),
                reqDto.ProductName(),
                reqDto.price(),
                reqDto.info(),
                mainImageDto,
                addSideImageDtos,
                removeSideImageDtos,
                savedDetailAndOptionInfoDto,
                productDetailUpdateDtos.updateQuantityDtos(),
                productDetailUpdateDtos.deleteDetailIds()
        );

        outboxCommandPort.syncUpdateEsEvent(productUpdateEventDto);
        outboxCommandPort.syncUpdateMongoEvent(productUpdateEventDto);

    }


}
