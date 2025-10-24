package com.space.munova.product.domain.Repository;

import com.space.munova.product.domain.ProductImage;
import com.space.munova.product.domain.enums.ProductImageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long>  {

    @Query("SELECT pi FROM ProductImage pi " +
            "WHERE pi.product.id = :productId " +
            "AND pi.isDeleted = false")
    List<ProductImage> findByProductId(Long productId);
}
