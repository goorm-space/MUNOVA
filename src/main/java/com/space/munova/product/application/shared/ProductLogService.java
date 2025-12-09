package com.space.munova.product.application.shared;


import com.space.munova.product.domain.*;
import com.space.munova.product.domain.Repository.ProductSearchLogRepository;
import com.space.munova.recommend.infra.RedisStreamProducer;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductLogService {

    private final ProductSearchLogRepository productSearchLogRepository;
    private final RedisStreamProducer logProducer;


    @Transactional(readOnly = false)
    public void saveProductClickLog(Long productId) {
        Long memberId = JwtHelper.getMemberId();
        Map<String, Object> logData = Map.of(
                "event_type", "product_detail_view",
                "service", "product",
                "member_id", memberId,
                "data", Map.of(
                        "product_id", productId
                )
        );
        logProducer.sendLogAsync(RedisStreamProducer.StreamType.PRODUCT, logData);

    }


    @Transactional
    public void saveSearchLog(Long categoryId, String keyword) {
        Long memberId = JwtHelper.getMemberId();

        ProductSearchLog log = ProductSearchLog.builder()
                .memberId(memberId)
                .searchDetail(keyword != null ? keyword : "")
                .searchCategoryId(categoryId)
                .build();

        productSearchLogRepository.save(log);

    }
}
