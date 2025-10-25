package com.space.munova.product.domain.Repository;

import com.space.munova.member.entity.Member;
import com.space.munova.product.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface CartRepository extends JpaRepository<Cart, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Cart c " +
            "SET c.isDeleted = true " +
            "WHERE c.productDetail.id IN :productDetailIds")
    void deleteByProductDetailIds(List<Long> productDetailIds);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Cart c " +
            "SET c.isDeleted = true " +
            "WHERE c.id IN :cartIds " +
            "AND c.member.id = :memberId")
    void deleteByCartIdsAndMemberId(List<Long> cartIds, Long memberId);


}
