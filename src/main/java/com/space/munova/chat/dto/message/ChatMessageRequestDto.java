package com.space.munova.chat.dto.message;

import com.space.munova.chat.enums.MessageType;

public record ChatMessageRequestDto(

        Long chatId,
        Long senderId,
        MessageType messageType,
        String content,
        Long clientTs
) {
    public static ChatMessageRequestDto of(Long chatId, Long senderId, MessageType messageType, String content, Long clientTs) {
        return new ChatMessageRequestDto(chatId, senderId, messageType, content, clientTs);
    }
}
