package com.space.munova.product.domain.Repository;

import com.space.munova.product.application.dto.ProductOptionInfoDto;
import com.space.munova.product.domain.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductDetailRepository extends JpaRepository<ProductDetail, Long>, ProductDetailRepositoryCustom {

    @Query("SELECT new com.space.munova.product.application.dto.ProductOptionInfoDto(o.id, pd.id, o.optionType, o.optionName,  pd.quantity) FROM Product p " +
            "JOIN ProductDetail pd " +
            "ON p.id = pd.product.id " +
            "LEFT JOIN ProductOptionMapping pom " +
            "ON pd.id = pom.productDetail.id " +
            "LEFT JOIN Option o " +
            "ON o.id = pom.option.id " +
            "WHERE p.id = :productId " +
            "AND pd.isDeleted = false ")
    List<ProductOptionInfoDto> findProductDetailAndOptionsByProductId(Long productId);
}
