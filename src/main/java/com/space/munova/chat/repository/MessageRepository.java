package com.space.munova.chat.repository;

import com.space.munova.chat.dto.message.ChatMessageViewDto;
import com.space.munova.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT new com.space.munova.chat.dto.message.ChatMessageViewDto" +
            "(m.content, m.type, u.username, m.createdAt) " +
            "FROM Message m " +
            "JOIN m.userId u " +
            "WHERE m.chatId.id = :chatId " +
            "ORDER BY m.createdAt ASC")
    List<ChatMessageViewDto> findAllByChatId(@Param("chatId") Long chatId);
}
