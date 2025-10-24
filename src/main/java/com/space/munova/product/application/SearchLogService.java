package com.space.munova.product.application;

import com.space.munova.product.domain.ProductSearchLog;
import com.space.munova.product.domain.Repository.ProductSearchLogRepository;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchLogService {

    private final ProductSearchLogRepository productSearchLogRepository;
    private final JwtHelper jwtHelper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSearchLog(Long categoryId, String keyword) {
        try {
            Long memberId = jwtHelper.getMemberId();

            ProductSearchLog log = ProductSearchLog.builder()
                    .memberId(memberId)
                    .searchDetail(keyword != null ? keyword : "")
                    .searchCategoryId(categoryId)
                    .build();
            productSearchLogRepository.save(log);
        } catch (Exception e) {
            System.out.println("검색 로그 저장 실패: " + e.getMessage());
        }
    }
}