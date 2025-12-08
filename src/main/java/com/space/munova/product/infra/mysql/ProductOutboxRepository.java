package com.space.munova.product.infra.mysql;

import com.space.munova.product.domain.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.util.List;

public interface ProductOutboxRepository extends JpaRepository<ProductOutbox, Long> {

    @Query("SELECT o FROM ProductOutbox o " +
            "WHERE o.status = :status " +
            "ORDER BY o.createdAt ASC " +
            "LIMIT 1000")
    List<ProductOutbox> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
