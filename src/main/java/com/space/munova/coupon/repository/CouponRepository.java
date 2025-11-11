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

    @Query("SELECT COUNT(c) > 0 FROM Coupon c " +
            "WHERE c.memberId = :memberId " +
            "AND c.couponDetail.id = :couponDetailId")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    boolean existsByMemberIdAndCouponDetailIdWithLock(
            @Param("memberId") Long memberId,
            @Param("couponDetailId") Long couponDetailId
    );

    // CouponDetail까지 함께 조회
    @EntityGraph(attributePaths = {"couponDetail"})
    Optional<Coupon> findWithCouponDetailById(Long couponId);
}
