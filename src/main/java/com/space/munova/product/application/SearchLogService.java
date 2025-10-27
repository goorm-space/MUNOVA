package com.space.munova.product.application;

import com.space.munova.product.domain.ProductSearchLog;
import com.space.munova.product.domain.Repository.ProductSearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.space.munova.security.jwt.JwtHelper.getMemberId;

@Service
@RequiredArgsConstructor
@Slf4j // 로거 사용 선언
public class SearchLogService {

    private final ProductSearchLogRepository productSearchLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSearchLog(Long categoryId, String keyword) {
    Long memberId = getMemberId();

    ProductSearchLog log = ProductSearchLog.builder()
            .memberId(memberId)
            .searchDetail(keyword != null ? keyword : "")
            .searchCategoryId(categoryId)
            .build();
    productSearchLogRepository.save(log);
    }
}