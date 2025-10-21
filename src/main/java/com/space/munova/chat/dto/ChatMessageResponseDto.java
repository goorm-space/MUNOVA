package com.space.munova.chat.dto;


import java.time.LocalDateTime;

public class ChatMessageResponseDto {

    private Long chatId;

    private Long senderId;

    private String content;

    private LocalDateTime createdAt;
}
