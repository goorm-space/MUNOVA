package com.space.munova.product.application;

import com.space.munova.product.domain.ProductClickLog;
import com.space.munova.product.domain.Repository.ProductClickLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.space.munova.security.jwt.JwtHelper.getMemberId;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductClickLogService {

    private final ProductClickLogRepository productClickLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveProductClickLog(Long productId) {
        Long memberId = getMemberId();
        ProductClickLog log = ProductClickLog.builder()
                .memberId(memberId)
                .productId(productId)
                .build();

        productClickLogRepository.save(log);
    }
}