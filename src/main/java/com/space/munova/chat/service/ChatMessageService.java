package com.space.munova.chat.service;

import com.space.munova.chat.dto.message.ChatMessageRequestDto;
import com.space.munova.chat.dto.message.ChatMessageResponseDto;
import com.space.munova.chat.dto.message.ChatMessageViewDto;

import java.util.List;

public interface ChatMessageService {

    ChatMessageResponseDto createChatMessage(ChatMessageRequestDto chatMessageRequest, Long chatId);

    List<ChatMessageViewDto> getMessagesByChatId(Long chatId);

}