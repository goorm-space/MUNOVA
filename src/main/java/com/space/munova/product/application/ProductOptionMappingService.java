package com.space.munova.product.application;

import com.space.munova.product.domain.ProductOptionMapping;
import com.space.munova.product.domain.Repository.OptionRepository;
import com.space.munova.product.domain.Repository.ProductOptionMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductOptionMappingService {
    private final ProductOptionMappingRepository productOptionMappingRepository;



    public void saveProductOptionMapping(ProductOptionMapping productOptionMapping) {
        productOptionMappingRepository.save(productOptionMapping);
    }


    public void deleteByProductDetailIds(List<Long> productDetailIds) {
        productOptionMappingRepository.deleteProductOptionMappingByProductDetailId(productDetailIds);
    }
}
