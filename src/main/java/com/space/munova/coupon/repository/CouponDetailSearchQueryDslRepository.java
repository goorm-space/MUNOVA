package com.space.munova.coupon.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.space.munova.core.utils.QuerydslHelper;
import com.space.munova.coupon.dto.SearchCouponDetailParams;
import com.space.munova.coupon.entity.CouponDetail;
import com.space.munova.coupon.entity.QCouponDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CouponDetailSearchQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    private static final QCouponDetail couponDetail = QCouponDetail.couponDetail;

    // 쿠폰 검색
    public Page<CouponDetail> findByCouponDetailParams(Pageable pageable, Sort sort, SearchCouponDetailParams params) {
// 카운트 쿼리
        long totalSize = queryFactory
                .select(couponDetail.count())
                .from(couponDetail)
                .where(publishIdEq(params.publishId()))
                .fetch()
                .size();

        // 조회 쿼리
        List<CouponDetail> coupons = queryFactory
                .select(couponDetail)
                .from(couponDetail)
                .where(publishIdEq(params.publishId()))
                .orderBy(toOrderSpecifiers(sort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(coupons, pageable, totalSize);
    }

    private BooleanBuilder publishIdEq(Long publishId) {
        return QuerydslHelper.nullSafeBuilder(() -> couponDetail.publisherId.eq(publishId));
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
