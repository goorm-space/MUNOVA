package com.space.munova.chat.dto.message;


import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.Message;
import com.space.munova.chat.enums.MessageType;
import com.space.munova.member.entity.Member;

import java.time.LocalDateTime;

public record ChatMessageResponseDto(

        Long chatId,
        Long senderId,
        String username,
        String content,
        LocalDateTime createdAt,
        MessageType messageType
) {
    public static ChatMessageResponseDto of(Chat chat, Member member, Message message) {
        return new ChatMessageResponseDto(chat.getId(), member.getId(), member.getUsername(), message.getContent(), message.getCreatedAt(), message.getType());
    }
}
