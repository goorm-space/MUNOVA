package com.space.munova.chat.repository;

import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import com.space.munova.chat.enums.ChatUserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    // 채팅방 아이디, type으로 조회
    Optional<Chat> findByIdAndType(Long id, ChatType type);

    // 존재하는 이름인지?
    boolean existsByName(String name);

    // 1:1 채팅방 조회, OPENED 되어 있는지 확인
    @Query("SELECT c " +
            "FROM Chat c " +
            "WHERE c.id = :chatId " +
            "AND c.status = com.space.munova.chat.enums.ChatStatus.OPENED " +
            "AND c.type = :chatType ")
    Optional<Chat> findChatByIdAndType(
            @Param("chatId") Long chatId,
            @Param("chatType") ChatType chatType);


    // 내가 그룹 채팅방 목록 확인용, 상태 상관 x
    // 판매자 만든 1:1 채팅방 목록 확인, 상태 상관 x
    @Query("SELECT c " +
            "FROM ChatMember cm " +
            "JOIN cm.chatId c " +
            "LEFT JOIN FETCH c.chatMembers " +
            "LEFT JOIN FETCH c.chatTags " +
            "WHERE cm.memberId.id = :memberId " +
            "AND cm.chatMemberType = :chatUserType " +
            "AND c.type = :chatType " +
            "ORDER BY COALESCE(c.lastMessageTime, c.createdAt) DESC")
    List<Chat> findByChatTypeAndChatUserType(
            @Param("memberId") Long memberId,
            @Param("chatType") ChatType chatType,
            @Param("chatUserType") ChatUserType chatUserType);

    // 1:1 채팅 조회(구매자, OPENED)
    @Query("SELECT c " +
            "FROM ChatMember cm " +
            "JOIN cm.chatId c " +
            "WHERE cm.memberId.id = :memberId " +
            "AND c.status = :chatStatus " +
            "AND c.type = :chatType " +
            "AND cm.chatMemberType = :chatUserType " +
            "ORDER BY COALESCE(c.lastMessageTime, c.createdAt) DESC")
    List<Chat> findByChatTypeAndChatStatus (
            @Param("memberId") Long memberId,
            @Param("chatType") ChatType chatType,
            @Param("chatUserType") ChatUserType chatUserType,
            @Param("chatStatus") ChatStatus chatStatus);



}
