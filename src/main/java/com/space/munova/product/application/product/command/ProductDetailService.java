package com.space.munova.product.application.product.command;

import com.space.munova.product.application.product.command.dto.SavedDetailAndOptionInfoDto;
import com.space.munova.product.application.product.command.dto.ShoeOptionDto;
import com.space.munova.product.application.product.command.dto.UpdateQuantityDto;
import com.space.munova.product.application.product.command.exception.ProductException;
import com.space.munova.product.domain.Option;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.ProductDetail;
import com.space.munova.product.domain.ProductOptionMapping;
import com.space.munova.product.domain.Repository.ProductDetailRepository;
import com.space.munova.product.application.product.command.exception.ProductDetailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductDetailService {

    private final ProductDetailRepository productDetailRepository;
    private final OptionCommandService optionCommandService;
    private final ProductOptionMappingCommandService productOptionMappingCommandService;


    public SavedDetailAndOptionInfoDto saveProductDetailAndOption(Product product, List<ShoeOptionDto> dtos) {

        List<ProductDetail> savedProductDetails = new ArrayList<>();
        List<ProductOptionMapping> savedProductOptionMappings = new ArrayList<>();

        dtos.forEach(dto -> {
            Long colorId = dto.colorId();
            Long sizeId = dto.sizeId();
            int quantity = dto.quantity();

            /// 디테일 생성
            ProductDetail productDetail = ProductDetail.createDefaultProductDetail(product, quantity);
            ProductDetail savedProductDetail = productDetailRepository.save(productDetail);
            savedProductDetails.add(savedProductDetail);
            List<ProductOptionMapping> optionMappings = createOptionMappings(savedProductDetail, colorId, sizeId);
            savedProductOptionMappings.addAll(optionMappings);
        });

        return new SavedDetailAndOptionInfoDto(savedProductDetails, savedProductOptionMappings);
    }


    public List<Long> deleteProductDetailByProductId(List<Long> productIds) {

        List<ProductDetail> productDetails = productDetailRepository.findAllByProductId(productIds);
        List<Long> productDetailIds = productDetails.stream()
                .map(ProductDetail::getId)
                .toList();

        /// 디테일 아이디를 가진 매핑 테이플 데이터 논리삭제
        productOptionMappingCommandService.deleteByProductDetailIds(productDetailIds);

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

           int rowCount = productDetailRepository.updateQuantity(updateQuantityDto.detailId(), updateQuantityDto.quantity());

           if(rowCount == 0) throw ProductDetailException.badRequest("상품 업데이트 실패.");
        }

    }

    public void deleteProductDetailByIds(List<Long> deleteDetailIds) {
        int rowCount = productDetailRepository.deleteProductDetailByIds(deleteDetailIds);
        if(rowCount != deleteDetailIds.size()) {
            throw ProductDetailException.badRequest("상품 업데이트 실패.");
        }
    }



    private List<ProductOptionMapping> createOptionMappings(
                                      ProductDetail savedProductDetail,
                                      Long colorId,
                                      Long sizeId) {

        List<ProductOptionMapping> savedProductOptionMappings = new ArrayList<>();

        Option colorOption = optionCommandService.findById(colorId);
        Option sizeOption = optionCommandService.findById(sizeId);

        ///  칼라옵션매핑 생성
        ProductOptionMapping colorOptionMapping =
                ProductOptionMapping.createDefaultProductOptionMapping(colorOption, savedProductDetail);
        productOptionMappingCommandService.saveProductOptionMapping(colorOptionMapping);

        ///  사이즈옵션매핑 생성
        ProductOptionMapping sizeOptionMapping =
                ProductOptionMapping.createDefaultProductOptionMapping(sizeOption, savedProductDetail);
        productOptionMappingCommandService.saveProductOptionMapping(sizeOptionMapping);

        savedProductOptionMappings.add(colorOptionMapping);
        savedProductOptionMappings.add(sizeOptionMapping);

        return savedProductOptionMappings;
    }

}
