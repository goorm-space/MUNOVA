package com.space.munova.chat.repository;

import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.enums.ChatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("SELECT c " +
            "FROM Chat c " +
            "WHERE c.id = :chatId " +
            "AND c.status = com.space.munova.chat.enums.ChatStatus.OPENED " +
            "AND c.type = com.space.munova.chat.enums.ChatType.GROUP ")
    Optional<Chat> findOpenedChatById(@Param("chatId") Long chatId);


    Optional<Chat> findByIdAndType(Long id, ChatType type);

    boolean existsByName(String name);
}
