package com.space.munova.chat.dto.message;


import com.space.munova.chat.entity.Message;
import com.space.munova.chat.enums.MessageType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChatMessageResponseDto {

    private Long chatId;

    private Long senderId;

    private String content;

    private LocalDateTime createdAt;

    private MessageType messageType;

    public ChatMessageResponseDto(Long chatId, Long senderId, Message message) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.content = message.getContent();
        this.createdAt = message.getCreatedAt();
        this.messageType = message.getType();
    }
}
