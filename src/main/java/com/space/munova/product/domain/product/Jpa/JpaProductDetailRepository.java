package com.space.munova.product.domain.product.Jpa;

import com.space.munova.product.domain.product.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProductDetailRepository extends JpaRepository<ProductDetail, Long> {

}
