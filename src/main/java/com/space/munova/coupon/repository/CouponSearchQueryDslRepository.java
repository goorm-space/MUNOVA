package com.space.munova.coupon.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.space.munova.coupon.dto.CouponStatus;
import com.space.munova.coupon.dto.SearchCouponParams;
import com.space.munova.coupon.entity.Coupon;
import com.space.munova.coupon.entity.QCoupon;
import com.space.munova.coupon.entity.QCouponDetail;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponSearchQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    private static final QCoupon coupon = QCoupon.coupon;
    private static final QCouponDetail couponDetail = QCouponDetail.couponDetail;

    // 쿠폰 검색
    public Page<Coupon> findByCouponParams(Pageable pageable, Sort sort, SearchCouponParams params) {
        // 카운트 쿼리
        Long totalSize = queryFactory
                .select(coupon.count())
                .from(coupon)
                .innerJoin(coupon.couponDetail, couponDetail)
                .where(statusEq(params.status()), memberIdEq())
                .fetchOne();

        totalSize = Optional.ofNullable(totalSize).orElse(0L);

        // 조회 쿼리
        List<Coupon> coupons = queryFactory
                .selectFrom(coupon)
                .innerJoin(coupon.couponDetail, couponDetail)
                .where(statusEq(params.status()), memberIdEq())
                .orderBy(toOrderSpecifiers(sort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(coupons, pageable, totalSize);
    }

    private BooleanBuilder memberIdEq() {
        Long memberId = JwtHelper.getMemberId();
        return new BooleanBuilder(coupon.memberId.eq(memberId));
    }

    private BooleanBuilder statusEq(String status) {
        if (status == null) {
            return new BooleanBuilder();
        }
        CouponStatus couponStatus = CouponStatus.valueOf(status);
        return new BooleanBuilder(coupon.status.eq(couponStatus));
    }

    // 정렬
    private OrderSpecifier<?>[] toOrderSpecifiers(Sort sort) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        sort.stream().forEach(order -> {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            PathBuilder<Coupon> pathBuilder = new PathBuilder<>(coupon.getType(), coupon.getMetadata());
            orders.add(new OrderSpecifier<>(direction, pathBuilder.getString(order.getProperty())));
        });

        return orders.toArray(new OrderSpecifier[0]);
    }
}
