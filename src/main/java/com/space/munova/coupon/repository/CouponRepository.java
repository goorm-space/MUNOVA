package com.space.munova.coupon.repository;

import com.space.munova.coupon.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    boolean existsByMemberIdAndCouponDetailId(Long memberId, Long couponDetailId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select count(c.id) from Coupon c where c.couponDetail.id = :couponDetailId and c.memberId = :memberId")
    Long findDuplicateIssuedCouponWithLock(@Param("couponDetailId") Long couponDetailId, @Param("memberId") Long memberId);

    // CouponDetail까지 함께 조회
    @EntityGraph(attributePaths = {"couponDetail"})
    Optional<Coupon> findWithCouponDetailById(Long couponId);
}
