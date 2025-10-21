package com.space.munova.chat.repository;

import com.space.munova.chat.entity.Chat;
import com.space.munova.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("SELECT c FROM Chat c WHERE c.userId.id = :buyerId ORDER BY c.lastMessageTime DESC")
    List<Chat> findAllByUserIdOrderByLastMessageTimeDesc(@Param("buyerId") Long buyerId);
}
