package com.space.munova.chat.controller;


import com.space.munova.chat.dto.OneToOneChatItemDto;
import com.space.munova.chat.dto.OneToOneChatRequestDto;
import com.space.munova.chat.dto.OneToOneChatResponseDto;
import com.space.munova.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatRoomController {

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ChatRoomService chatService;

    // 일대일 채팅방 생성
    @PostMapping("/chat/one-to-one")
    public ResponseEntity<OneToOneChatResponseDto> createChatRoom(
            @RequestBody OneToOneChatRequestDto requestChatRoomDto)  {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.createOneToOneChatRoom(requestChatRoomDto));
    }

    // 구매자의 1:1 문의 채팅 목록 확인
    @GetMapping("/chat/one-to-one/{userId}")
    public ResponseEntity<List<OneToOneChatItemDto>> getChatRoom(@PathVariable Long userId) {
        return ResponseEntity.ok(chatService.getOneToOneChatRooms(userId));
    }


}
