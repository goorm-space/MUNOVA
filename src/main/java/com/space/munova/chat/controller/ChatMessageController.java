package com.space.munova.chat.controller;

import com.space.munova.chat.dto.message.ChatMessageRequestDto;
import com.space.munova.chat.dto.message.ChatMessageResponseDto;
import com.space.munova.chat.dto.message.ChatMessageViewDto;
import com.space.munova.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatMessageController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat.sendMessage/{chatId}")
    public ChatMessageResponseDto sendMessage(
            @DestinationVariable Long chatId,
            @Payload ChatMessageRequestDto chatMessageRequestDto) {

        log.info("Sending message to chat: {}", chatMessageRequestDto);
        log.info("chatId: {}", chatId);
        // 메시지 전송
        ChatMessageResponseDto chatMessage = chatMessageService.createChatMessage(chatMessageRequestDto, chatId);

        // 해당 경로를 구독하고 있는 클라이언트가 있으면 메시지 전달, 없으면 버려짐
        simpMessagingTemplate.convertAndSend("/msub/topic/" + chatId, chatMessage);
        return chatMessage;
    }

    // 채팅 메시지 조회
    @GetMapping("/api/chat/messages/{chatId}")
    public List<ChatMessageViewDto> getMessages(
            @PathVariable Long chatId) {
        return chatMessageService.getMessagesByChatId(chatId);
    }

}
