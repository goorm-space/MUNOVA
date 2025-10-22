package com.space.munova.chat.dto;

import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.Message;
import com.space.munova.chat.enums.MessageType;
import com.space.munova.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class ChatMessageViewDto {
    private Long messageId;

    private String content;

    private MessageType type;   // TEXT, IMAGE

    private Long chatId;

    private Long userId;

    private String username;

    private LocalDateTime createdAt;

    public ChatMessageViewDto(Message message){
        this.messageId = message.getId();
        this.content = message.getContent();
        this.type = message.getType();
        this.chatId = message.getChatId().getId();
        this.userId = message.getUserId().getId();
        this.username = message.getUserId().getUsername();
        this.createdAt = message.getCreatedAt();
    }

}
