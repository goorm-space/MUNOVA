package com.space.munova.product.application;

import com.space.munova.product.domain.Repository.ProductLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductLikeService {

    private final ProductLikeRepository productLikeRepository;

    public void deleteProductLikeByProductId(List<Long> productIds) {
        productLikeRepository.deleteAllByProductIds(productIds);
    }
}
