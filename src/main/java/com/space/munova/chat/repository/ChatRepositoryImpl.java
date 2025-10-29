package com.space.munova.chat.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.QChat;
import com.space.munova.chat.entity.QChatMember;
import com.space.munova.chat.entity.QChatTag;
import com.space.munova.chat.enums.ChatType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRepositoryImpl implements ChatRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Chat> findByNameAndTags(String keyword, List<Long> tagIds, Long memberId) {
        QChat chat = QChat.chat;
        QChatTag chatTag = QChatTag.chatTag;
        QChatMember chatMember = QChatMember.chatMember;

        JPAQuery<Chat> query = queryFactory
                .selectFrom(chat);

        if (tagIds != null && !tagIds.isEmpty()) {
            query.join(chat.chatTags, chatTag)
                    .on(tagsIn(tagIds));
        }

        if (memberId != null) {
            query.join(chat.chatMembers, chatMember)
                    .on(chatMember.memberId.id.eq(memberId));
        }

        return query
                .where(
                        keywordLike(keyword),
                        chat.type.eq(ChatType.GROUP)
                )
                .distinct()
                .fetch();
    }

    @Override
    public Optional<Chat> findGroupChatDetailById(Long chatId) {
        QChat chat = QChat.chat;
        QChatMember chatMember = QChatMember.chatMember;
        QChatTag chatTag = QChatTag.chatTag;

        Chat result = queryFactory
                .selectFrom(chat)
                .leftJoin(chat.chatMembers, chatMember).fetchJoin()
                .leftJoin(chat.chatTags, chatTag).fetchJoin()
                .where(
                        chat.id.eq(chatId),
                        chat.type.eq(ChatType.GROUP)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    private BooleanExpression tagsIn(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return null;
        return QChatTag.chatTag.productCategoryId.id.in(tagIds);
    }

    private BooleanExpression keywordLike(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return QChat.chat.name.containsIgnoreCase(keyword);
    }
}
