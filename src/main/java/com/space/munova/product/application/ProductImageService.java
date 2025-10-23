package com.space.munova.product.application;

import com.space.munova.product.application.dto.UploadFile;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.ProductImage;
import com.space.munova.product.domain.Repository.ProductImageRepository;
import com.space.munova.product.domain.enums.ProductImageType;
import com.space.munova.product.infra.file.FileStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final FileStore fileStore;

    public void saveMainImg(MultipartFile mainImgFile, Product product) throws IOException {
        UploadFile mainImgInfo = fileStore.storeFile(mainImgFile);
        ProductImage mainImg = ProductImage.createDefaultProductImage(ProductImageType.MAIN, mainImgInfo.getOriginName(), mainImgInfo.getSavedName(), product);
        productImageRepository.save(mainImg);
    }

    public void saveSideImg(List<MultipartFile> sideImgFile,  Product product) throws IOException {
        List<UploadFile> sideImgInfos = fileStore.storeFiles(sideImgFile);
        List<ProductImage> productImages = new ArrayList<>();
        sideImgInfos.forEach(sideImg -> {
            ProductImage sideImage = ProductImage.createDefaultProductImage(ProductImageType.SIDE, sideImg.getOriginName(), sideImg.getSavedName(), product);
            productImages.add(sideImage);
        });
        productImageRepository.saveAll(productImages);
    }


}
