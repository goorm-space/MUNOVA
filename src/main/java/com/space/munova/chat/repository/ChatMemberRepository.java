package com.space.munova.chat.repository;

import com.space.munova.chat.dto.ChatItemDto;
import com.space.munova.chat.entity.ChatMember;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import com.space.munova.chat.enums.ChatUserType;
import com.space.munova.member.entity.Member;
import com.space.munova.product.domain.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMemberRepository extends CrudRepository<ChatMember, Long> {

    Optional<ChatMember> findByMemberIdAndProductId(Member memberId, Product productId);

    @Query("SELECT new com.space.munova.chat.dto.ChatItemDto" +
            "(c.id, c.name, c.lastMessageContent, c.lastMessageTime) " +
            "FROM ChatMember cm " +
            "JOIN cm.chatId c " +
            "WHERE cm.memberId.id = :memberId " +
            "AND c.status = :chatStatus " +
            "AND (c.type = :chatType) " +
            "AND (cm.chatMemberType = :chatUserType) " +
            "ORDER BY c.lastMessageTime DESC")
    List<ChatItemDto> findAllChats(
            @Param("memberId") Long memberId,
            @Param("chatType") ChatType chatType,
            @Param("chatUserType") ChatUserType chatUserType,
            @Param("chatStatus") ChatStatus chatStatus);


    @Query("SELECT new com.space.munova.chat.dto.ChatItemDto" +
            "(c.id, c.name, c.lastMessageContent, c.lastMessageTime) " +
            "FROM ChatMember cm " +
            "JOIN cm.chatId c " +
            "WHERE cm.memberId = :memberId " +
            "AND c.status = :chatStatus " +
            "AND (c.type = :chatType) " +
            "ORDER BY c.lastMessageTime DESC")
    List<ChatItemDto> findAllGroupChats(
            @Param("memberId") Long memberId,
            @Param("chatType") ChatType chatType,
            @Param("chatStatus") ChatStatus chatStatus);


    // 참여자 여부 확인 (메시지 전송/조회용)
    @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END " +
            "FROM ChatMember cm " +
            "JOIN cm.chatId c " +
            "WHERE cm.chatId.id = :chatId " +
            "AND cm.memberId.id = :memberId " +
            "AND c.status = :chatStatus")
    boolean existsBy(
            @Param("chatId") Long chatId,
            @Param("memberId") Long memberId,
            @Param("chatStatus") ChatStatus chatStatus);

    // 참여자 여부 확인 (메시지 전송/조회용)
    @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END " +
            "FROM ChatMember cm " +
            "JOIN cm.chatId c " +
            "WHERE cm.chatId.id = :chatId " +
            "AND cm.memberId.id = :memberId " +
            "AND c.status = :chatStatus")
    boolean isOwner(
            @Param("chatId") Long chatId,
            @Param("memberId") Long memberId,
            @Param("chatStatus") ChatStatus chatStatus);


    @Query("SELECT cm " +
            "FROM ChatMember cm " +
            "JOIN cm.chatId c " +
            "WHERE cm.chatId.id = :chatId " +
            "AND cm.memberId.id = :memberId " +
            "AND c.status = :chatStatus " +
            "AND c.type = :chatType " +
            "AND cm.chatMemberType = :chatUserType")
    Optional<ChatMember> findChatMember(
            @Param("chatId") Long chatId,
            @Param("memberId") Long memberId,
            @Param("chatStatus") ChatStatus chatStatus,
            @Param("chatType") ChatType chatType,
            @Param("chatUserType") ChatUserType chatUserType);


}
