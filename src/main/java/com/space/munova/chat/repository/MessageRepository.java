package com.space.munova.chat.repository;

import com.space.munova.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m " +
            "JOIN FETCH m.chatId c " +
            "JOIN FETCH m.userId u " +
            "WHERE c.id = :chatId ORDER BY m.createdAt ASC")
    List<Message> findAllByChatIdWithChat(@Param("chatId") Long chatId);
}
