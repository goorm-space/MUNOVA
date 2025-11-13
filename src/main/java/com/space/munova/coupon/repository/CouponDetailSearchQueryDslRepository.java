package com.space.munova.coupon.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.space.munova.core.utils.QuerydslHelper;
import com.space.munova.coupon.dto.SearchCouponDetailParams;
import com.space.munova.coupon.dto.SearchCouponDetailResponse;
import com.space.munova.coupon.dto.SearchEventCouponResponse;
import com.space.munova.coupon.entity.CouponDetail;
import com.space.munova.coupon.entity.QCoupon;
import com.space.munova.coupon.entity.QCouponDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponDetailSearchQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    private static final QCoupon coupon = QCoupon.coupon;
    private static final QCouponDetail couponDetail = QCouponDetail.couponDetail;

    // 쿠폰 검색
    public Page<SearchCouponDetailResponse> findByCouponDetailParams(
            Pageable pageable, Sort sort, SearchCouponDetailParams params
    ) {
        // 카운트 쿼리
        Long totalSize = queryFactory
                .select(couponDetail.count())
                .from(couponDetail)
                .where(publishIdEq(params.publishId()))
                .fetchOne();

        totalSize = Optional.ofNullable(totalSize).orElse(0L);

        // 조회 쿼리
        List<SearchCouponDetailResponse> coupons = queryFactory
                .select(Projections.constructor(
                        SearchCouponDetailResponse.class,
                        couponDetail.id.as("couponDetailId"),
                        couponDetail.quantity.as("quantity"),
                        couponDetail.remainQuantity.as("remainQuantity"),
                        couponDetail.couponName.as("couponName"),
                        couponDetail.discountPolicy.as("discountPolicy"),
                        couponDetail.publishedAt.as("publishedAt"),
                        couponDetail.expiredAt.as("expiredAt")
                ))
                .from(couponDetail)
                .where(publishIdEq(params.publishId()))
                .orderBy(toOrderSpecifiers(sort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(coupons, pageable, totalSize);
    }

    // 이벤트 쿠폰 검색
    public Page<SearchEventCouponResponse> findByEventCoupon(Pageable pageable, Sort sort, Long memberId) {
        // 카운트 쿼리
        Long totalSize = queryFactory
                .select(couponDetail.count())
                .from(couponDetail)
                .leftJoin(coupon)
                .on(coupon.couponDetail.id.eq(couponDetail.id)
                        .and(coupon.memberId.eq(memberId)))
                .where(quantityGreaterThanZero(), isInPublishExpiredAt())
                .fetchOne();

        totalSize = Optional.ofNullable(totalSize).orElse(0L);

        // 조회 쿼리
        List<SearchEventCouponResponse> coupons = queryFactory
                .select(Projections.constructor(
                        SearchEventCouponResponse.class,
                        couponDetail.id.as("couponDetailId"),
                        couponDetail.quantity.as("quantity"),
                        couponDetail.remainQuantity.as("remainQuantity"),
                        couponDetail.couponName.as("couponName"),
                        couponDetail.discountPolicy.as("discountPolicy"),
                        couponDetail.publishedAt.as("publishedAt"),
                        couponDetail.expiredAt.as("expiredAt"),
                        coupon.id.isNotNull().as("isAlreadyIssued")
                ))
                .from(couponDetail)
                .leftJoin(coupon)
                .on(coupon.couponDetail.id.eq(couponDetail.id)
                        .and(coupon.memberId.eq(memberId)))
                .where(quantityGreaterThanZero(), isInPublishExpiredAt())
                .orderBy(toOrderSpecifiers(sort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(coupons, pageable, totalSize);
    }

    private BooleanBuilder publishIdEq(Long publishId) {
        return QuerydslHelper.nullSafeBuilder(() -> couponDetail.publisherId.eq(publishId));
    }

    private BooleanBuilder quantityGreaterThanZero() {
        return QuerydslHelper.nullSafeBuilder(() -> couponDetail.quantity.gt(0));
    }

    private BooleanBuilder isInPublishExpiredAt() {
        LocalDateTime now = LocalDateTime.now();
        return QuerydslHelper.nullSafeBuilder(() ->
                couponDetail.publishedAt.loe(now).and(couponDetail.expiredAt.goe(now))
        );
    }

    // 정렬
    private OrderSpecifier<?>[] toOrderSpecifiers(Sort sort) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        sort.stream().forEach(order -> {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            PathBuilder<CouponDetail> pathBuilder = new PathBuilder<>(couponDetail.getType(), couponDetail.getMetadata());
            orders.add(new OrderSpecifier<>(direction, pathBuilder.getString(order.getProperty())));
        });

        return orders.toArray(new OrderSpecifier[0]);
    }
}
