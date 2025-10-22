package com.space.munova.recommend.repository;

import com.space.munova.product.domain.Category;
import com.space.munova.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findTop4ByCategoryAndIdNotOrderByIdAsc(Category category, Long excludeId);


    List<Product> findTop4ByCategoryIdAndIdNotOrderByIdAsc(Category category, Long id);
}