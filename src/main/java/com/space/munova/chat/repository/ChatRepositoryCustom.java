package com.space.munova.chat.repository;

import com.space.munova.chat.entity.Chat;

import java.util.List;

public interface ChatRepositoryCustom {
    List<Chat> findByNameAndTags(String keyword, List<Long> categoryIds, Long memberId);
}
