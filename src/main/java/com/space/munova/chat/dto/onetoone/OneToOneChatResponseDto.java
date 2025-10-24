package com.space.munova.chat.dto.onetoone;

import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.enums.ChatStatus;

import java.time.LocalDateTime;


public record OneToOneChatResponseDto(
        Long chatId,
        Long sellerId, // 판매자
        Long buyerId,   // 구매자
        String name,   // 채팅방 이름
        LocalDateTime createdAt,
        ChatStatus chatStatus
) {
    public static OneToOneChatResponseDto of(Chat chat, Long buyer, Long seller) {
        return new OneToOneChatResponseDto(
                chat.getId(),
                seller,
                buyer,
                chat.getName(),
                chat.getCreatedAt(),
                chat.getStatus());
    }
}
