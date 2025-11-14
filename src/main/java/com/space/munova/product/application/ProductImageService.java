package com.space.munova.product.application;


import com.space.munova.product.application.dto.ProductImageDto;
import com.space.munova.product.application.dto.ProductSideImgInfoDto;
import com.space.munova.product.application.dto.UploadFile;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.ProductImage;
import com.space.munova.product.domain.Repository.ProductImageRepository;
import com.space.munova.product.domain.enums.ProductImageType;
import com.space.munova.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.data.util.Optionals.ifPresentOrElse;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final S3Service s3Service;

    public void saveMainImg(MultipartFile mainImgFile, Product product) throws IOException {
        String imgUrl = s3Service.uploadFile(mainImgFile);
        ProductImage mainImg = ProductImage.createDefaultProductImage(ProductImageType.MAIN, imgUrl, product);
        productImageRepository.save(mainImg);
    }

    public void updateMainImg(MultipartFile mainImgFile, Product product) throws IOException {

        String imgUrl = s3Service.uploadFile(mainImgFile);

        Optional<ProductImage> mainImgByProductId = productImageRepository.findMainImgByProductId(product.getId(), ProductImageType.MAIN);

        if(mainImgByProductId.isPresent()) { ///  이미지정보 있을경우
            ProductImage mainImg = mainImgByProductId.get();
            s3Service.deleteFile(mainImg.getImgUrl());
            // 이미지 업데이트
            mainImg.updateProductImage(imgUrl);
        } else { ///  이미지 정보없을경우

            ProductImage productImage = ProductImage.createDefaultProductImage(ProductImageType.MAIN, imgUrl, product);
            productImageRepository.save(productImage);
        }
    }

    public void saveSideImg(List<MultipartFile> sideImgFile,  Product product) throws IOException {

        List<String> imgUrls = s3Service.uploadFiles(sideImgFile);

        List<ProductImage> productImages = new ArrayList<>();
        imgUrls.forEach(sideImgUrl -> {
            ProductImage sideImage = ProductImage.createDefaultProductImage(ProductImageType.SIDE, sideImgUrl, product);
            productImages.add(sideImage);
        });
        productImageRepository.saveAll(productImages);
    }

    /// 상품 이미지 제거 메서드
    public void deleteImagesByImgIds(List<Long> imgIds, Long productId) {

        if(imgIds == null || imgIds.isEmpty()) {
            return;
        }

        List<String> imgUrls = productImageRepository.findImgUrlsByIdsAndProductId(imgIds, productId);

        log.info(imgUrls.get(0));
        s3Service.deleteFiles(imgUrls);

        productImageRepository.deleteProductImgsByImgIdsAndProductId(imgIds, productId);
    }



    public ProductImageDto findProductImageDtoByProductId(Long productId) {

        List<ProductImage> productImages = productImageRepository.findByProductId(productId);

        return ProductImageDto.fromProductImages(productImages);
    }

    public void deleteImagesByProductIds(List<Long> productIds) {
        /// 상품이미지 논리적 삭제
        List<String> imgUrls = new ArrayList<>();
        productImageRepository.findByProductIds(productIds)
                .forEach(productImage -> {
                    imgUrls.add(productImage.getImgUrl());
                    productImage.deleteImage();
                });

        ///  실제 파일 제거
        s3Service.deleteFiles(imgUrls);
    }


}
