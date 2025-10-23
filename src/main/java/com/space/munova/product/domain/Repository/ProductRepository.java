package com.space.munova.product.domain.Repository;

import com.space.munova.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {
    List<Product> findTop4ByCategory_IdAndIdNotOrderByIdAsc(Long categoryId, Long excludeId);
}
