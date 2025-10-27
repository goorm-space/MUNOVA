package com.space.munova.product.domain.Repository;

import com.space.munova.member.entity.Member;
import com.space.munova.product.application.dto.cart.CartItemBasicInfoDto;
import com.space.munova.product.application.dto.cart.CartItemInfoDto;
import com.space.munova.product.domain.Cart;
import com.space.munova.product.domain.ProductDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface CartRepository extends JpaRepository<Cart, Long>, CartRepositoryCustom {
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


    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM Cart c " +
            "WHERE c.member.id = :memberId " +
            "AND c.productDetail.id = :productDetailId " +
            "AND c.isDeleted = false")
    boolean existsByMemberIdAndProductDetailId(Long memberId, Long productDetailId);

    @Query("SELECT c FROM Cart c " +
            "WHERE c.productDetail.id = :productDetailId " +
            "AND c.member.id = :memberId " +
            "AND c.isDeleted = false")
    Optional<Cart> findByProductDetailIdAndMemberId(Long productDetailId, Long memberId);

    List<Cart> findByMemberId(Long memberId);

//    @Query("SELECT new com.space.munova.product.application.dto.cart.CartItemBasicInfoDto() " +
//            "FROM Cart c " +
//            "JOIN " +
//            "WHERE c.member.id = :memberId " +
//            "AND c.isDeleted = false")
//    List<CartItemBasicInfoDto> findCartItemInfoByMemberId(Long memberId);
}
