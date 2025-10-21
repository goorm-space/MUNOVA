package com.space.munova.chat.service;

import com.space.munova.chat.dto.ChatMessageRequestDto;
import com.space.munova.chat.dto.ChatMessageResponseDto;
import com.space.munova.chat.dto.ChatMessageViewDto;

import java.util.List;

public interface ChatMessageService {

    ChatMessageResponseDto createChatMessage(ChatMessageRequestDto chatMessageRequest);

    List<ChatMessageViewDto> getMessagesByChatId(Long chatId);

}