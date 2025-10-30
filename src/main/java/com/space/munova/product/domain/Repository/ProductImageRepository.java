package com.space.munova.product.domain.Repository;

import com.space.munova.product.domain.ProductImage;
import com.space.munova.product.domain.enums.ProductImageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long>  {

    @Query("SELECT pi FROM ProductImage pi " +
            "WHERE pi.product.id = :productId " +
            "AND pi.isDeleted = false")
    List<ProductImage> findByProductId(Long productId);


    /**
     * clearAutomatically = true: 쿼리 실행 후 1차 캐시를 자동으로 비워 데이터 불일치를 방지합니다.
     * flushAutomatically = true: 이 쿼리 실행 전, 1차 캐시의 다른 변경 사항을 DB에 먼저 반영(flush)합니다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductImage pi " +
            "SET pi.isDeleted = true " +
            "WHERE pi.product.id IN :productIds")
    void deleteAllByProductIds(List<Long> productIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductImage " +
            "SET isDeleted = true " +
            "WHERE id IN :imgIds " +
            "AND product.id = :productId")
    void deleteProductImgsByImgIdsAndProductId(List<Long> imgIds, Long productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductImage pi " +
            "SET pi.originName = :originName," +
            "    pi.savedName = :savedName  " +
            "WHERE pi.product.id = :productId " +
            "AND pi.imageType = :imageType ")
    void updateProductImageByProduct(Long productId, String originName, String savedName,  ProductImageType imageType);
}
