package com.space.munova.chat.controller;

import com.space.munova.chat.dto.message.ChatMessageRequestDto;
import com.space.munova.chat.dto.message.ChatMessageResponseDto;
import com.space.munova.chat.dto.message.ChatMessageViewDto;
import com.space.munova.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatMessageService chatMessageService;

    // 임시로 chatMessageRequestDto에 전송하는 사람 아이디가 들어 있다고 가정
    @MessageMapping("/chat/send")
    public ChatMessageResponseDto sendMessage(@Payload ChatMessageRequestDto chatMessageRequestDto){

        // 메시지 전송
        ChatMessageResponseDto chatMessage = chatMessageService.createChatMessage(chatMessageRequestDto);

        // 해당 경로를 구독하고 있는 클라이언트가 있으면 메시지 전달, 없으면 버려짐
        simpMessagingTemplate.convertAndSend("/msub/topic/" + chatMessageRequestDto.getChatId(), chatMessage);
        return chatMessage;
    }

    // 채팅 메시지 조회
    @GetMapping("/chat/messages/{memberId}/{chatId}")
    public List<ChatMessageViewDto> getMessages(
            @PathVariable Long memberId,
            @PathVariable Long chatId){
        return chatMessageService.getMessagesByChatId(chatId, memberId);
    }

}
