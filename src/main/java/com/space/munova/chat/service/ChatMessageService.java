package com.space.munova.chat.service;

import com.space.munova.chat.dto.ChatMessageRequestDto;
import com.space.munova.chat.dto.ChatMessageResponseDto;

public interface ChatMessageService {

    ChatMessageResponseDto createChatMessage(ChatMessageRequestDto chatMessageRequest);
}
