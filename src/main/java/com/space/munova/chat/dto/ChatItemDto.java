package com.space.munova.chat.dto;

import com.space.munova.chat.entity.Chat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@AllArgsConstructor
public class ChatItemDto {

    private Long chatId;

    private String name;

    private String lastMessageContent;

    private LocalDateTime lastMessageTime;

    public ChatItemDto(Chat chat) {
        this.chatId = chat.getId();
        this.name = chat.getName();
        this.lastMessageContent = chat.getLastMessageContent();
        this.lastMessageTime = chat.getLastMessageTime();
    }
}
