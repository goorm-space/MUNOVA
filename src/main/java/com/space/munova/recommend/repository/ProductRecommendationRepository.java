package com.space.munova.recommend.repository;

import com.space.munova.product.domain.Product;
import com.space.munova.recommend.domain.ProductRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRecommendationRepository extends JpaRepository<ProductRecommendation, Long> {
    List<ProductRecommendation> findBySourceProductId(Long sourceProductId);
    void deleteBySourceProduct(Product product);
}