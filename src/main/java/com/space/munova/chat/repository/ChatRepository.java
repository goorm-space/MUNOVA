package com.space.munova.chat.repository;

import com.space.munova.chat.dto.ChatItemDto;
import com.space.munova.chat.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("SELECT new com.space.munova.chat.dto.ChatItemDto" +
            "(c.id, c.name, c.lastMessageContent, c.lastMessageTime)" +
            "FROM Chat c " +
            "WHERE c.userId.id = :buyerId " +
            "AND c.status = 'OPENED' " +
            "AND c.type = com.space.munova.chat.enums.ChatType.ONE_ON_ONE " +
            "ORDER BY c.lastMessageTime DESC")
    List<ChatItemDto> findAllByUserIdOrderByLastMessageTimeDesc(@Param("buyerId") Long buyerId);
}
