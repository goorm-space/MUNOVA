package com.space.munova.product.domain.Repository;

import com.space.munova.product.domain.ProductLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductLikeRepository extends JpaRepository<ProductLike, Long>, ProductLikeRepositoryCustom {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductLike pl " +
            "SET pl.isDeleted = true " +
            "WHERE pl.product.id IN :productIds")
    void deleteAllByProductIds(List<Long> productIds);



    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductLike pl " +
            "SET pl.isDeleted = true " +
            "WHERE pl.product.id = :productId " +
            "AND pl.member.id = :memberId " +
            "AND pl.isDeleted = false")
    int deleteAllByProductIdsAndMemberId(Long productId, Long memberId);

    boolean existsByProductIdAndMemberIdAndIsDeletedFalse(Long productId, Long memberId);

    ProductLike findLikeProductByProductIdAndMemberId(Long productId, Long memberId);
}
