package com.space.munova.product.application;

import com.space.munova.product.application.dto.*;
import com.space.munova.product.application.exception.ProductException;
import com.space.munova.product.domain.Option;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.product.domain.ProductOptionMapping;
import com.space.munova.product.domain.Repository.ProductDetailRepository;
import com.space.munova.product.application.exception.ProductDetailException;
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



    public void saveProductDetailAndOption(Product product, List<ShoeOptionDto> dtos) {
        dtos.forEach(dto -> {
            Long colorId = dto.colorId();
            Long sizeId = dto.sizeId();
            int quantity = dto.quantity();

            /// 디테일 생성
            ProductDetail productDetail = ProductDetail.createDefaultProductDetail(product, quantity);
            ProductDetail savedProductDetail = productDetailRepository.save(productDetail);
            createOptionMappings(savedProductDetail, colorId, sizeId);
        });
    }


    //*
    // 상품아이디를 통해 상품디테일에 종속된 옵션 조회후 상품상세조회를 위한 DTO로 분류하여 반환 메서드
    // @parma - productId
    // */
    public List<ProductDetailInfoDto> findProductDetailInfoDtoByProductId(Long productId) {
        ///  1급 컬랙션으로 만들어버림.
        ProductDetailOptions productDetailOptions = new ProductDetailOptions(productDetailRepository.findProductDetailAndOptionsByProductId(productId));

        return  productDetailOptions.toProductDetailInfoList();
    }


    public List<Long> deleteProductDetailByProductId(List<Long> productIds) {

        List<ProductDetail> productDetails = productDetailRepository.findAllByProductId(productIds);
        List<Long> productDetailIds = productDetails.stream()
                .map(ProductDetail::getId)
                .toList();

        /// 디테일 아이디를 가진 매핑 테이플 데이터 논리삭제
        productOptionMappingService.deleteByProductDetailIds(productDetailIds);

        /// 디테일 아이디를 가진 디테일 테이블 데이터 논리 삭제
        productDetailRepository.deleteProductDetailByIds(productDetailIds);
        return productDetailIds;
    }


    public ProductDetail findById(Long detailId) {
        return productDetailRepository.findById(detailId).orElseThrow(ProductException::badRequestException);
    }

    public ProductDetail getProductDetailWithPessimisticLock(Long productDetailId) {

        return productDetailRepository.findByIdWithPessimisticLock(productDetailId)
                .orElseThrow(ProductDetailException::notFoundException);
    }

    @Transactional(readOnly = false)
    public ProductDetail deductStock(Long productDetailId, int quantity) {
        ProductDetail productDetail = getProductDetailWithPessimisticLock(productDetailId);

        if (productDetail.getQuantity() == 0) {
            throw ProductDetailException.noStockException("product_detail_id: " + productDetailId);
        } else if (productDetail.getQuantity() < quantity) {
            throw ProductDetailException.stockInsufficientException("product_detail_id: " + productDetailId + ", 요청: " + quantity + ", 재고: " + productDetail.getQuantity());
        }

        productDetail.deductStock(quantity);

        return productDetail;
    }

    public Long findProductIdByDetailId(Long detailId) {
        return productDetailRepository
                .findProductIdById(detailId)
                .orElseThrow(ProductDetailException::notFoundException);
    }

    @Transactional
    public void increaseStock(Long productDetailId, int cancelQuantity) {
        ProductDetail productDetail = getProductDetailWithPessimisticLock(productDetailId);

        productDetail.increaseStock(cancelQuantity);
    }




    public void updateQuantity(List<UpdateQuantityDto> updateQuantityDtos) {

        for(UpdateQuantityDto updateQuantityDto : updateQuantityDtos) {
            productDetailRepository.updateQuantity(updateQuantityDto.detailId(), updateQuantityDto.quantity());
        }
    }

    public void deleteProductDetailByIds(List<Long> deleteDetailIds) {
        productDetailRepository.deleteProductDetailByIds(deleteDetailIds);
    }



    private void createOptionMappings(ProductDetail savedProductDetail, Long colorId, Long sizeId) {
        Option colorOption = optionService.findById(colorId);
        Option sizeOption = optionService.findById(sizeId);

        ///  칼라옵션매핑 생성
        ProductOptionMapping colorOptionMapping =
                ProductOptionMapping.createDefaultProductOptionMapping(colorOption, savedProductDetail);
        productOptionMappingService.saveProductOptionMapping(colorOptionMapping);

        ///  사이즈옵션매핑 생성
        ProductOptionMapping sizeOptionMapping =
                ProductOptionMapping.createDefaultProductOptionMapping(sizeOption, savedProductDetail);
        productOptionMappingService.saveProductOptionMapping(sizeOptionMapping);
    }


/*   사용 x -> 이후에 사용할수도 있을것 같아 주석처리.

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
*/
}
