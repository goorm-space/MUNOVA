package com.space.munova.product.domain.Repository;

import com.space.munova.product.domain.Option;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOptionRepository extends JpaRepository<Option, Long> {
}
