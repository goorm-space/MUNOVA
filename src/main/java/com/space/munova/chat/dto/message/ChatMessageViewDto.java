package com.space.munova.chat.dto.message;

import com.space.munova.chat.enums.MessageType;

import java.time.LocalDateTime;

public record ChatMessageViewDto(
        String content,
        MessageType type,   // TEXT, IMAGE
//    Long userId,
        String username,
        LocalDateTime createdAt
) {
    public ChatMessageViewDto(String content, MessageType type, String username, LocalDateTime createdAt) {
        this.content = content;
        this.type = type;
        this.username = username;
        this.createdAt = createdAt;
    }
}
