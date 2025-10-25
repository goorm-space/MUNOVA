package com.space.munova.product.application;

import com.space.munova.product.domain.Repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    public void deleteByProductDetailIds(List<Long> productDetailIds) {
        cartRepository.deleteByProductDetailIds(productDetailIds);
    }
}
