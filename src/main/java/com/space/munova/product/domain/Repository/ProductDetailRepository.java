package com.space.munova.product.domain.Repository;

import com.space.munova.product.application.dto.ProductOptionInfoDto;
import com.space.munova.product.application.dto.cart.CartItemOptionInfoDto;
import com.space.munova.product.application.dto.cart.ProductInfoForCartDto;
import com.space.munova.product.domain.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductDetailRepository extends JpaRepository<ProductDetail, Long>, ProductDetailRepositoryCustom {

    @Query("SELECT new com.space.munova.product.application.dto.ProductOptionInfoDto(o.id, pd.id, o.optionType, o.optionName,  pd.quantity) " +
            "FROM Product p " +
            "JOIN ProductDetail pd " +
            "ON p.id = pd.product.id " +
            "LEFT JOIN ProductOptionMapping pom " +
            "ON pd.id = pom.productDetail.id " +
            "LEFT JOIN Option o " +
            "ON o.id = pom.option.id " +
            "WHERE p.id = :productId " +
            "AND pd.isDeleted = false ")
    List<ProductOptionInfoDto> findProductDetailAndOptionsByProductId(Long productId);

//    @Query("SELECT pd.id " +
//            "FROM ProductDetail pd " +
//            "WHERE pd.product.id = :productId")
//    List<ProductDetail> findByProductId(Long productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductDetail pd " +
            "SET pd.isDeleted = true " +
            "WHERE pd.id IN :productIds")
    void deleteProductDetailByIds(List<Long> productIds);

    @Query("SELECT pd " +
            "FROM ProductDetail pd " +
            "WHERE pd.product.id IN :productIds")
    List<ProductDetail> findAllByProductId(List<Long> productIds);

    @Query("SELECT new com.space.munova.product.application.dto.cart.ProductInfoForCartDto(pd.id, p.id, p.name, p.price, pi.savedName, b.brandName) " +
            "FROM ProductDetail pd " +
            "JOIN Product p " +
            "ON p.id = pd.product.id " +
            "JOIN ProductImage pi " +
            "ON pi.product.id = p.id " +
            "JOIN Brand b " +
            "ON b.id = p.brand.id " +
            "WHERE pd.id IN :productDetailIds " +
            "AND pd.isDeleted = false " +
            "AND pi.imageType = 'MAIN'")
    List<ProductInfoForCartDto> findProductDetailInfosForCart(List<Long> productDetailIds);

    @Query("SELECT new com.space.munova.product.application.dto.cart.CartItemOptionInfoDto( ) " +
            "FROM ProductDetail pd " +
            "LEFT JOIN ProductOptionMapping po " +
            "ON po.productDetail.id = pd.id " +
            "LEFT JOIN Option o " +
            "ON o.id = po.option.id " +
            "WHERE pd.id IN :productDetailIds " +
            "AND pd.isDeleted = false ")
    List<CartItemOptionInfoDto> findProductDetailOptionForCart(List<Long> productDetailIds);
}
