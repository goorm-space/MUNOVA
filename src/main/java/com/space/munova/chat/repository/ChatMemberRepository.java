package com.space.munova.chat.repository;

import com.space.munova.chat.entity.ChatMember;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import com.space.munova.chat.enums.ChatUserType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatMemberRepository extends CrudRepository<ChatMember, Long> {

    @Query("SELECT cm " +
            "FROM ChatMember cm " +
            "JOIN cm.chatId c " +
            "WHERE cm.memberId.id = :memberId " +
                "AND c.productId.id = :productId " +
                "AND c.status = com.space.munova.chat.enums.ChatStatus.OPENED")
    Optional<ChatMember> findExistingChatRoom(
            @Param("memberId") Long memberId,
            @Param("productId") Long productId);

    // 참여자 여부 확인 (메시지 전송/조회용)
    @Query("SELECT COUNT(cm) > 0 " +
            "FROM ChatMember cm " +
            "JOIN cm.chatId c " +
            "WHERE cm.chatId.id = :chatId " +
                "AND cm.memberId.id = :memberId " +
                "AND c.status = :chatStatus")
    boolean existsMemberInChat(
            @Param("chatId") Long chatId,
            @Param("memberId") Long memberId,
            @Param("chatStatus") ChatStatus chatStatus);

    @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END " +
            "FROM ChatMember cm " +
            "WHERE cm.chatId.id = :chatId " +
            "AND cm.memberId.id = :memberId")
    boolean existsChatMemberAndMemberIdBy(
            @Param("chatId") Long chatId,
            @Param("memberId") Long memberId);

    @Query("SELECT cm " +
            "FROM ChatMember cm " +
            "JOIN FETCH cm.chatId c " +
            "WHERE cm.chatId.id = :chatId " +
                "AND cm.memberId.id = :memberId " +
                "AND (:chatStatus IS NULL OR c.status = :chatStatus) " +
                "AND c.type = :chatType " +
                "AND cm.chatMemberType = :chatUserType")
    Optional<ChatMember> findChatMember(
            @Param("chatId") Long chatId,
            @Param("memberId") Long memberId,
            @Param("chatStatus") ChatStatus chatStatus,
            @Param("chatType") ChatType chatType,
            @Param("chatUserType") ChatUserType chatUserType);
}
