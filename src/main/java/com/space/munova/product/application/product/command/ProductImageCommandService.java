package com.space.munova.product.application.product.command;


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

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageCommandService {

    private final ProductImageRepository productImageRepository;
    private final S3Service s3Service;

    public ProductImage saveMainImg(MultipartFile mainImgFile, Product product) throws IOException {
        String imgUrl = s3Service.uploadFile(mainImgFile);
        ProductImage mainImg = ProductImage.createDefaultProductImage(ProductImageType.MAIN, imgUrl, product);
        return productImageRepository.save(mainImg);
    }


    public ProductImage updateMainImg(MultipartFile mainImgFile, Product product) throws IOException {

        String imgUrl = s3Service.uploadFile(mainImgFile);

        Optional<ProductImage> mainImgByProductId = productImageRepository.findMainImgByProductId(product.getId(), ProductImageType.MAIN);

        ProductImage updatedMainImg = null;

        if(mainImgByProductId.isPresent()) { ///  이미지정보 있을경우
            ProductImage mainImg = mainImgByProductId.get();
            s3Service.deleteFile(mainImg.getImgUrl());
            // 이미지 업데이트
            mainImg.updateProductImage(imgUrl);
            updatedMainImg = mainImg;

        } else { ///  이미지 정보없을경우

            ProductImage productImage = ProductImage.createDefaultProductImage(ProductImageType.MAIN, imgUrl, product);
            ProductImage savedImg = productImageRepository.save(productImage);
            updatedMainImg = savedImg;
        }

        return updatedMainImg;
    }

    public List<ProductImage> saveSideImg(List<MultipartFile> sideImgFile,  Product product) throws IOException {

        List<String> imgUrls = s3Service.uploadFiles(sideImgFile);

        List<ProductImage> productImages = new ArrayList<>();
        imgUrls.forEach(sideImgUrl -> {
            ProductImage sideImage = ProductImage.createDefaultProductImage(ProductImageType.SIDE, sideImgUrl, product);
            productImages.add(sideImage);
        });
        return productImageRepository.saveAll(productImages);
    }

    /// 상품 이미지 제거 메서드
    public List<ProductImage> deleteImagesByImgIds(List<Long> imgIds, Long productId) {

        List<ProductImage> deleteImgs = productImageRepository.findImgUrlsByIdsAndProductId(imgIds, productId);

        List<String> imgUrls = deleteImgs.stream()
                .map(ProductImage::getImgUrl)
                .toList();

        if (!imgUrls.isEmpty()) {
            s3Service.deleteFiles(imgUrls);
        }

        productImageRepository.deleteAll(deleteImgs);
        return deleteImgs;
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
