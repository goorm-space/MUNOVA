package com.space.munova.chat.repository;

import com.space.munova.chat.dto.ChatItemDto;
import com.space.munova.chat.entity.Chat;
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

    @Query("SELECT c " +
            "FROM Chat c " +
            "WHERE c.id = :chatId " +
            "AND c.status = com.space.munova.chat.enums.ChatStatus.OPENED " +
            "AND c.type = com.space.munova.chat.enums.ChatType.GROUP ")
    Optional<Chat> findOpenedGroupChatById(@Param("chatId") Long chatId);

    @Query("SELECT c " +
            "FROM Chat c " +
            "WHERE c.id = :chatId " +
            "AND c.status = com.space.munova.chat.enums.ChatStatus.OPENED")
    Optional<Chat> findOpenedChatById(@Param("chatId") Long chatId);

    Optional<Chat> findByIdAndType(Long id, ChatType type);

    boolean existsByName(String name);

    @Query("SELECT new com.space.munova.chat.dto.ChatItemDto" +
            "(c.id, c.name, c.lastMessageContent, c.lastMessageTime) " +
            "FROM Chat c " +
            "WHERE c.status = com.space.munova.chat.enums.ChatStatus.OPENED " +
            "AND c.type = com.space.munova.chat.enums.ChatType.GROUP " +
            "ORDER BY c.lastMessageTime DESC")
    List<ChatItemDto> findAllGroupChats();


    @Query("SELECT c " +
            "FROM ChatMember cm " +
            "JOIN cm.chatId c " +
            "WHERE cm.memberId.id = :memberId " +
            "AND cm.chatMemberType = :chatUserType " +
            "AND c.type = :chatType")
    List<Chat> findByMemberIdAndChatUserType(
            @Param("memberId") Long memberId,
            @Param("chatType") ChatType chatType,
            @Param("chatUserType") ChatUserType chatUserType);
}
