package com.space.munova.product.application.dto;

import com.space.munova.product.domain.enums.OptionCategory;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

///  1급 컬랙션
@Getter
public class ProductDetailOptions {

    private final List<ProductOptionInfoDto> options;

    public ProductDetailOptions(List<ProductOptionInfoDto> options) {
        this.options = (options != null) ? new ArrayList<>(options) : new ArrayList<>();
    }

    public List<ProductDetailInfoDto> toProductDetailInfoList() {

        /// 상품색상 아이디 아래에 여러개의 상품디테일아이디가 있다.
        /// 상품색상아이디(키), 상품디테일아이디 (밸류)
        Map<ColorOptionDto, List<Long>> classifiedDetailByColorMap = new HashMap<>();
        /// 디테일 아이디가 가지고있는 사이즈 리스트
        Map<Long, ProductDetailAndSizeDto> detailIdMappedSizeOptionMap = new HashMap<>();

        this.classifyOptionDatas(classifiedDetailByColorMap, detailIdMappedSizeOptionMap);

        return this.createProductDetailInfoList(classifiedDetailByColorMap, detailIdMappedSizeOptionMap);
    }

    /// 데이터 분류 메서드
    private void classifyOptionDatas( Map<ColorOptionDto, List<Long>> classifiedDetailByColorMap, Map<Long, ProductDetailAndSizeDto> detailIdMappedSizeOptionMap) {

        this.options.forEach(dto -> {
            ///  옵션타입이 컬러일경우, 키값에 옵션ID를 저장하고 밸류에 해당 디테일리스트를 저장.
            if(dto.optionType().equals(OptionCategory.COLOR)) {

                /// 컬러를 기준으로 디테일아이디를 분류한다.
                classifyDetailByColor(dto, classifiedDetailByColorMap);
            }

            if(dto.optionType().equals(OptionCategory.SIZE)) {
                classifySizeOptionsByDetail(dto, detailIdMappedSizeOptionMap);
            }
        });
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
