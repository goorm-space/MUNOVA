package com.space.munova.chat.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.QChat;
import com.space.munova.chat.entity.QChatTag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRepositoryImpl implements ChatRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Chat> findByNameAndTags(String keyword, List<Long> categoryIds) {
        QChat chat = QChat.chat;
        QChatTag chatTag = QChatTag.chatTag;

        return queryFactory
                .selectFrom(chat)
                .join(chat.chatTags, chatTag)
                .where(
                        keywordLike(keyword),
                        tagsIn(categoryIds)
                )
                .distinct()
                .fetch();
    }

    private BooleanExpression tagsIn(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return null;
        return QChatTag.chatTag.productCategoryId.id.in(categoryIds);
    }

    private BooleanExpression keywordLike(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return QChat.chat.name.containsIgnoreCase(keyword);
    }
}
