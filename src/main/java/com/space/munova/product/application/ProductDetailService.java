package com.space.munova.product.application;

import com.space.munova.product.application.dto.*;
import com.space.munova.product.application.dto.cart.CartItemOptionInfoDto;
import com.space.munova.product.application.exception.ProductException;
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

import java.util.*;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductDetailService {

    private final ProductDetailRepository productDetailRepository;
    private final OptionService optionService;
    private final ProductOptionMappingService productOptionMappingService;
    //private final CartService cartService;

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


    //*
    // 상품아이디를 통해 상품디테일에 종속된 옵션 조회후 상품상세조회를 위한 DTO로 분류하여 반환 메서드
    // @parma - productId
    // */
    public List<ProductDetailInfoDto> findProductDetailInfoDtoByProductId(Long productId) {
        List<ProductOptionInfoDto> productOptionInfoDtos = productDetailRepository.findProductDetailAndOptionsByProductId(productId);

        /// 상품색상 아이디 아래에 여러개의 상품디테일아이디가 있다.
        /// 상품색상아이디(키), 상품디테일아이디 (밸류)
        Map<ColorOptionDto, List<Long>> classifiedDetailByColorMap = new HashMap<>();
        /// 디테일 아이디가 가지고있는 사이즈 리스트
        Map<Long, ProductDetailAndSizeDto> detailIdMappedSizeOptionMap = new HashMap<>();

        classifyOptionDatas(productOptionInfoDtos, classifiedDetailByColorMap, detailIdMappedSizeOptionMap);

        createProductDetailInfoList(classifiedDetailByColorMap, detailIdMappedSizeOptionMap);

        return  createProductDetailInfoList(classifiedDetailByColorMap, detailIdMappedSizeOptionMap);
    }


    public void deleteProductDetailByProductId(List<Long> productIds) {

        List<ProductDetail> productDetails = productDetailRepository.findAllByProductId(productIds);
        List<Long> productDetailIds = getProductDetailIds(productDetails);

        /// 디테일 아이디를 가진 매핑 테이플 데이터 논리삭제
        productOptionMappingService.deleteByProductDetailIds(productDetailIds);

        /// 디테일 아이디를 가진 카트 테이블 데이터 논리 삭제
        // cartService.deleteByProductDetailIds(productDetailIds);

        /// 디테일 아이디를 가진 디테일 테이블 데이터 논리 삭제
        productDetailRepository.deleteProductDetailByIds(productDetailIds);
    }

//    public List<ProductInfoForCartDto> findProductInfoByDetailIds(List<Long> productDetailIds) {
//
//        return productDetailRepository.findProductDetailInfosForCart(productDetailIds);
//    }

    public ProductDetail findById(Long detailId) {
        return productDetailRepository.findById(detailId).orElseThrow(ProductException::badRequestException);
    }


    private List<Long> getProductDetailIds(List<ProductDetail> productDetails) {
        List<Long> productDetailIds = new ArrayList<>();
        for (ProductDetail productDetail : productDetails) {
            productDetailIds.add(productDetail.getId());
        }
        return productDetailIds;
    }

    private List<ProductDetailInfoDto> createProductDetailInfoList(Map<ColorOptionDto, List<Long>> classifiedDetailByColorMap, Map<Long, ProductDetailAndSizeDto> detailIdMappedSizeOptionMap) {
        List<ProductDetailInfoDto> productDetailInfoDtos = new ArrayList<>();

        classifiedDetailByColorMap.entrySet().forEach(entry -> {
            List<ProductDetailAndSizeDto> productDetailAndSizeDtos = new ArrayList<>();
            for(Long detailId : entry.getValue()) {
                ProductDetailAndSizeDto productDetailAndSizeDto = detailIdMappedSizeOptionMap.get(detailId);


                productDetailAndSizeDtos.add(productDetailAndSizeDto);
            }

            ProductDetailInfoDto productDetailInfoDto = new ProductDetailInfoDto(entry.getKey(), productDetailAndSizeDtos);
            productDetailInfoDtos.add(productDetailInfoDto);
        });

        return productDetailInfoDtos;
    }


    /// 데이터 분류 메서드
    private void classifyOptionDatas(List<ProductOptionInfoDto> productOptionInfoDtos, Map<ColorOptionDto, List<Long>> classifiedDetailByColorMap, Map<Long, ProductDetailAndSizeDto> detailIdMappedSizeOptionMap) {
        for (ProductOptionInfoDto dto : productOptionInfoDtos) {

            ///  옵션타입이 컬러일경우, 키값에 옵션ID를 저장하고 밸류에 해당 디테일리스트를 저장.
            if(dto.optionType().equals(OptionCategory.COLOR)) {

                /// 컬러를 기준으로 디테일아이디를 분류한다.
                classifyDetailByColor(dto, classifiedDetailByColorMap);
            }

            if(dto.optionType().equals(OptionCategory.SIZE)) {
                classifySizeOptionsByDetail(dto, detailIdMappedSizeOptionMap);
            }

        }
    }


    /// 디테일 기준으로 사이즈옵션 분류
    private void classifySizeOptionsByDetail(ProductOptionInfoDto dto, Map<Long, ProductDetailAndSizeDto> detailIdMappedSizeOptionMap) {
        ProductDetailAndSizeDto productDetailAndSizeDto = new ProductDetailAndSizeDto(dto.detailId(), dto.optionId(), dto.optionType().name(), dto.optionName(), dto.quantity());
        detailIdMappedSizeOptionMap.put(dto.detailId(), productDetailAndSizeDto);
    }


    /// 컬러를 기준으로 디테일을 분류하는 메소드
    private void classifyDetailByColor(ProductOptionInfoDto dto, Map<ColorOptionDto, List<Long>> classifiedMap) {
        ColorOptionDto colorOptionDto = new ColorOptionDto(dto.optionId(), dto.optionType().name(), dto.optionName());
        if(classifiedMap.containsKey(colorOptionDto)) {
            List<Long> detailIds = classifiedMap.get(colorOptionDto);
            detailIds.add(dto.detailId());
            classifiedMap.put(colorOptionDto, detailIds);
        } else {
            List<Long> detailIds = new ArrayList<>();
            detailIds.add(dto.detailId());
            classifiedMap.put(colorOptionDto, detailIds);
        }
    }



}
