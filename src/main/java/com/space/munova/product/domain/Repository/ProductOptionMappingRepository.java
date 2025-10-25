package com.space.munova.product.domain.Repository;

import com.space.munova.product.domain.ProductOptionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductOptionMappingRepository extends JpaRepository<ProductOptionMapping, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductOptionMapping po " +
            "SET po.isDeleted = true " +
            "WHERE po.productDetail.id IN :productDetailIds")
    void deleteProductOptionMappingByProductDetailId(List<Long> productDetailIds);
}
