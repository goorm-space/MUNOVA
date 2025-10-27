package com.space.munova.chat.dto.message;

import com.space.munova.chat.enums.MessageType;

public record ChatMessageRequestDto(

        Long senderId,
        MessageType messageType,
        String content
) {
    public static ChatMessageRequestDto of(Long senderId, MessageType messageType, String content) {
        return new ChatMessageRequestDto(senderId, messageType, content);
    }
}
