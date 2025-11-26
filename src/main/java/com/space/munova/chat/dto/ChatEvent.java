package com.space.munova.chat.dto;

public record ChatEvent(
        Long chatId,
        String jsonPayload
) { }
