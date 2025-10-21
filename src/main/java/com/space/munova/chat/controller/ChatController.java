package com.space.munova.chat.controller;


import com.space.munova.chat.dto.OneToOneChatRoomRequestDto;
import com.space.munova.chat.dto.OneToOneChatRoomResponseDto;
import com.space.munova.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate  simpMessagingTemplate;
    private final ChatRoomService chatService;

    // 일대일 채팅방 생성
    @PostMapping("/chat/one-to-one")
    public ResponseEntity<OneToOneChatRoomResponseDto> createChatRoom(
            @RequestBody OneToOneChatRoomRequestDto requestChatRoomDto)  {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.createOneToOneChatRoom(requestChatRoomDto));
    }
}
