package com.space.munova.product.domain.Repository;

import com.space.munova.product.domain.ProductClickLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductClickLogRepository extends JpaRepository<ProductClickLog, Long> {
}
