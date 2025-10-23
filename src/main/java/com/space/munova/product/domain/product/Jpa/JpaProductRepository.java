package com.space.munova.product.domain.product.Jpa;

import com.space.munova.product.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProductRepository extends JpaRepository<Product, Long> {


}
