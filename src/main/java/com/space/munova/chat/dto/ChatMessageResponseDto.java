package com.space.munova.chat.dto;


import com.space.munova.chat.entity.Message;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

public class ChatMessageResponseDto {

    private Long chatId;

    private Long senderId;

    private String content;

    private LocalDateTime createdAt;

    public ChatMessageResponseDto(Long chatId, Long senderId, Message message) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.content = message.getContent();
        this.createdAt = message.getCreatedAt();

    }
}
