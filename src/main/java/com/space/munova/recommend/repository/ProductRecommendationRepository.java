package com.space.munova.recommend.repository;

import com.space.munova.product.domain.Product;
import com.space.munova.recommend.domain.ProductRecommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRecommendationRepository extends JpaRepository<ProductRecommendation, Long> {
    Page<ProductRecommendation> findBySourceProductId(Long sourceProductId, Pageable pageable);
    void deleteBySourceProduct(Product product);
    Page<ProductRecommendation> findAll(Pageable pageable);
}