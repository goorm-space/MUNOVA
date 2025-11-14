package com.space.munova.product.domain.Repository;


import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.application.dto.ProductInfoDto;
import com.space.munova.product.domain.Product;
import com.space.munova.recommend.dto.RecommendProductResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {
    @Query("""
        SELECT new com.space.munova.product.application.dto.FindProductResponseDto(
            p.id,
            pi.imgUrl,
            b.brandName,
            p.name,
            p.price,
            p.likeCount,
            p.salesCount,
            p.createdAt
        )
        FROM Product p
        JOIN p.category c
        LEFT JOIN p.brand b
        LEFT JOIN ProductImage pi 
            ON pi.product.id = p.id AND pi.imageType = com.space.munova.product.domain.enums.ProductImageType.MAIN
        WHERE (c.refCategory.id = :refCategoryId OR c.id = :refCategoryId)
          AND p.id <> :excludeId
          AND p.isDeleted = false
        ORDER BY p.id ASC
    """)
    List<FindProductResponseDto> findSimilarProductsByCategory(Long refCategoryId, Long excludeId, Pageable pageable);

    @Query("SELECT new com.space.munova.product.application.dto.ProductInfoDto(p.id, c.id, b.brandName, p.name, p.info, p.price, p.likeCount, p.viewCount) " +
            "FROM Product p " +
            "LEFT JOIN Brand b " +
            "ON p.brand.id = b.id " +
            "LEFT JOIN Category c " +
            "ON c.id = p.category.id " +
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

    boolean existsByIdAndMemberIdAndIsDeletedFalse(Long productId , Long sellerId);

    @Query("SELECT p FROM Product p " +
            "WHERE p.isDeleted = false " +
            "AND p.id = :productId " +
            "AND p.member.id = :sellerId ")
    Optional<Product> findByIdAndMemberIdAndIsDeletedFalse(Long productId, Long sellerId);

    @Query("""
    SELECT new com.space.munova.product.application.dto.FindProductResponseDto(
        p.id,
        pi.imgUrl,
        b.brandName,
        p.name,
        p.price,
        p.likeCount,
        p.salesCount,
        p.createdAt
    )
    FROM Product p
    LEFT JOIN p.brand b
    LEFT JOIN ProductImage pi 
        ON pi.product.id = p.id 
        AND pi.imageType = com.space.munova.product.domain.enums.ProductImageType.MAIN
    WHERE p.id = :productId
      AND p.isDeleted = false
""")
    FindProductResponseDto findProductSummaryById(@Param("productId") Long productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p " +
            "SET p.likeCount = p.likeCount + 1 " +
            "WHERE p.id = :productId")
    void plusLikeCountByProductId(Long productId);

    @Query("SELECT p " +
            "FROM Product p " +
            "WHERE p.id iN :productIds " +
            "AND p.member.id = :sellerId " +
            "AND p.isDeleted = false")
    List<Product> findAllByIdAndMemberId(List<Long> productIds, Long sellerId);
}
