package com.space.munova.chat.dto.message;


import com.space.munova.chat.enums.MessageType;

import java.time.LocalDateTime;

public record ChatMessageResponseDto(

        Long chatId,
        Long senderId,
        String username,
        String content,
        LocalDateTime createdAt,
        MessageType messageType
) {
    public static ChatMessageResponseDto of(Long chatId, Long senderId, String username, String content, LocalDateTime createdAt, MessageType messageType) {
        return new ChatMessageResponseDto(chatId, senderId, username, content, createdAt, messageType);
    }
}
