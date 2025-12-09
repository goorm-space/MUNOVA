package com.space.munova.product.infra.mysql;

import com.space.munova.product.domain.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductOutboxRepository extends JpaRepository<ProductOutbox, Long> {

    // 상위 1000건까지 조회 (JPQL LIMIT 불가하므로 메서드 쿼리 사용)
    List<ProductOutbox> findTop1000ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
