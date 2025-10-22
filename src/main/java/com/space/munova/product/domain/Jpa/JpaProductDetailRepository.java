package com.space.munova.product.domain.Jpa;

import com.space.munova.product.domain.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProductDetailRepository extends JpaRepository<ProductDetail, Long> {

}
