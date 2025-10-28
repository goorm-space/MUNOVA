package com.space.munova.product.domain.Repository;


import com.space.munova.product.application.dto.ProductInfoDto;
import com.space.munova.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {
    List<Product> findTop4ByCategory_IdAndIdNotOrderByIdAsc(Long categoryId, Long excludeId);

    @Query("SELECT new com.space.munova.product.application.dto.ProductInfoDto(p.id, b.brandName, p.name, p.info, p.price) " +
            "FROM Product p " +
            "LEFT JOIN Brand b " +
            "ON p.brand.id = b.id " +
            "WHERE p.id = :productId " +
            "AND p.isDeleted = false")
    Optional<ProductInfoDto> findProductInfoById(Long productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p " +
            "SET p.viewCount = p.viewCount + 1 " +
            "WHERE p.id = :productId")
    void updateProductViewCount(Long productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p " +
            "SET p.isDeleted = true " +
            "WHERE p.id IN :productIds ")
    void deleteAllByProductIds(List<Long> productIds);

    Optional<Product> findByIdAndIsDeletedFalse(Long productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p " +
            "SET p.likeCount = p.likeCount - 1 " +
            "WHERE p.id = :productId " +
            "AND p.likeCount > 0")
    int minusLikeCountInProductIds(Long productId);
}
