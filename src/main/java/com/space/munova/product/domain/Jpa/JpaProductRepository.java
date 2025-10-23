package com.space.munova.product.domain.Jpa;

import com.space.munova.product.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProductRepository extends JpaRepository<Product, Long> {


}
