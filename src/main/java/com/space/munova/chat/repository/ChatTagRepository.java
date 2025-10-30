package com.space.munova.chat.repository;

import com.space.munova.chat.entity.ChatTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatTagRepository extends JpaRepository<ChatTag, Long> {
}
