package com.space.munova.product.application;

import com.space.munova.product.application.dto.ShoeOptionDto;
import com.space.munova.product.domain.Option;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.product.domain.ProductOptionMapping;
import com.space.munova.product.domain.Repository.ProductDetailRepository;
import com.space.munova.product.domain.enums.OptionCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductDetailService {

    private final ProductDetailRepository productDetailRepository;
    private final OptionService optionService;
    private final ProductOptionMappingService productOptionMappingService;

    public ProductDetail saveProductDetail(ProductDetail productDetail) {
        return productDetailRepository.save(productDetail);
    }

    public void saveProductDetailAndOption(Product product, List<ShoeOptionDto> dtos) {

        ///  사이즈 색상별 옵션 프로덕트옵션매핑, 상품디테일 생성 메서드
        for (ShoeOptionDto dto : dtos) {

            String color = dto.color();
            String size = dto.size();
            int quantity = dto.quantity();

            /// 디테일 생성
            ProductDetail productDetail = ProductDetail.createDefaultProductDetail(product, quantity);
            ProductDetail savedProductDetail = saveProductDetail(productDetail);

            /// 옵션생성
            saveOption(OptionCategory.COLOR, color, savedProductDetail);
            saveOption(OptionCategory.SIZE, size, savedProductDetail);
        }
    }

    /// 옵션 및 상품옵션매핑 데이터 생성로직
    /// 옵션이 없을경우 옵션 만들고 옵션저장후 매핑 테이블 저장
    /// 옵션이 있을경우 옵션 찾아온 후 매핑테이블저장.
    private void saveOption(OptionCategory optionCategory, String optionName, ProductDetail savedProductDetail) {
        if (!optionService.isExist(optionCategory, optionName)) {
            Option option = Option.createDefaultOption(optionCategory, optionName);
            Option savedOption = optionService.saveOption(option);
            ProductOptionMapping productOptionMapping = ProductOptionMapping.createDefaultProductOptionMapping(savedOption, savedProductDetail);
            productOptionMappingService.saveProductOptionMapping(productOptionMapping);
        } else {
            Option foundOption = optionService.findByCategoryAndName(optionCategory, optionName);
            ProductOptionMapping productOptionMapping = ProductOptionMapping.createDefaultProductOptionMapping(foundOption, savedProductDetail);
            productOptionMappingService.saveProductOptionMapping(productOptionMapping);
        }
    }
}
