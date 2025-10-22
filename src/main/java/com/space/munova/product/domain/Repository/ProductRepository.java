package com.space.munova.product.domain.Repository;

import com.space.munova.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> , ProductRepositoryCustom {
}
