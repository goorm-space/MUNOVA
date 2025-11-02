package com.space.munova.notification.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.space.munova.core.utils.QuerydslHelper;
import com.space.munova.notification.entity.Notification;
import com.space.munova.notification.entity.QNotification;
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
public class NotificationQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    private static final QNotification notification = QNotification.notification;

    // 알림 검색
    public Page<Notification> findNotifications(Pageable pageable, Sort sort, Long memberId) {
        // 카운트 쿼리
        Long totalSize = queryFactory
                .select(notification.count())
                .from(notification)
                .where(memberIdEq(memberId))
                .fetchOne();

        totalSize = Optional.ofNullable(totalSize).orElse(0L);

        // 조회 쿼리
        List<Notification> notifications = queryFactory
                .select(notification)
                .from(notification)
                .where(memberIdEq(memberId))
                .orderBy(toOrderSpecifiers(sort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(notifications, pageable, totalSize);
    }

    private BooleanBuilder memberIdEq(Long memberId) {
        return QuerydslHelper.nullSafeBuilder(() -> notification.memberId.eq(memberId));
    }

    // 정렬
    private OrderSpecifier<?>[] toOrderSpecifiers(Sort sort) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        sort.stream().forEach(order -> {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            PathBuilder<Notification> pathBuilder = new PathBuilder<>(notification.getType(), notification.getMetadata());
            orders.add(new OrderSpecifier<>(direction, pathBuilder.getString(order.getProperty())));
        });

        return orders.toArray(new OrderSpecifier[0]);
    }
}
