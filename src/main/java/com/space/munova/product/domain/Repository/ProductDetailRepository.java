package com.space.munova.product.domain.Repository;

import com.space.munova.product.application.dto.ProductOptionInfoDto;
import com.space.munova.product.domain.ProductDetail;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductDetail pd " +
            "SET pd.isDeleted = true " +
            "WHERE pd.id IN :productIds")
    void deleteProductDetailByIds(List<Long> productIds);

    @Query("SELECT pd " +
            "FROM ProductDetail pd " +
            "WHERE pd.product.id IN :productIds")
    List<ProductDetail> findAllByProductId(List<Long> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pd FROM ProductDetail pd WHERE pd.id = :id")
    Optional<ProductDetail> findByIdWithPessimisticLock(Long id);

    @Query("SELECT pd.product.id FROM ProductDetail pd WHERE pd.id = :id")
    Optional<Long> findProductIdById(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductDetail pd " +
            "SET pd.quantity = :quantity " +
            "WHERE pd.id = :detailId")
    void updateQuantity(Long detailId, int quantity);
}
