package com.space.munova.recommend.repository;

import com.space.munova.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findTop4ByCategory_IdAndIdNotOrderByIdAsc(Long categoryId, Long excludeId);
}