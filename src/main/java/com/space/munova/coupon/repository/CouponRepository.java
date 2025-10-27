package com.space.munova.coupon.repository;

import com.space.munova.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    boolean existsByMemberIdAndCouponDetailId(Long memberId, Long couponDetailId);

    // CouponDetail까지 함께 조회
    @EntityGraph(attributePaths = {"couponDetail"})
    Optional<Coupon> findWithCouponDetailById(Long couponId);
}
