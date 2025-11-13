package com.space.munova.coupon.repository;

import com.space.munova.coupon.entity.CouponDetail;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponDetailRepository extends JpaRepository<CouponDetail, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CouponDetail c where c.id = :couponDetailId")
    Optional<CouponDetail> findByIdWithLock(@Param("couponDetailId") Long couponDetailId);
}
