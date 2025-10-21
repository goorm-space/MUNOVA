package com.space.munova.chat.controller;

import com.space.munova.chat.dto.ChatMessageRequestDto;
import com.space.munova.chat.dto.ChatMessageResponseDto;
import com.space.munova.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatMessageService chatMessageService;

    // 임시로 chatMessageRequestDto에 전송하는 사람 아이디가 들어 있다고 가정하는게 낫나
    @MessageMapping("/chat/send")
    public ChatMessageRequestDto sendMessage(ChatMessageRequestDto chatMessageRequestDto){

        ChatMessageResponseDto chatMessage = chatMessageService.createChatMessage(chatMessageRequestDto);

        // 해당 경로를 구독하고 있는 클라이언트가 있으면 메시지 전달, 없으면 버려짐
        simpMessagingTemplate.convertAndSend("/msub/topic/" + chatMessageRequestDto.getChatId(), chatMessage);
        return chatMessageRequestDto;
    }
}
