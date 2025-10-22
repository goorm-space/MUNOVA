package com.space.munova.chat.controller;


import com.space.munova.chat.dto.group.GroupChatRequestDto;
import com.space.munova.chat.dto.group.GroupChatResponseDto;
import com.space.munova.chat.dto.ChatItemDto;
import com.space.munova.chat.dto.onetoone.OneToOneChatRequestDto;
import com.space.munova.chat.dto.onetoone.OneToOneChatResponseDto;
import com.space.munova.chat.service.ChatRoomService;
import com.space.munova.core.config.ResponseApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatRoomController {

    private final ChatRoomService chatService;

    // 문의하기 -> 1:1 채팅방 생성
    @PostMapping("/one-to-one/{memberId}/{productId}")
    public ResponseApi<OneToOneChatResponseDto> createOneToOneChatRoom(
            @PathVariable Long memberId,
            @PathVariable Long productId)  {
        return ResponseApi.ok(chatService.createOneToOneChatRoom(memberId, productId));
    }

    // 구매자의 1:1 문의 채팅 목록 확인
    @GetMapping("/one-to-one/{buyerId}")
    public ResponseApi<List<ChatItemDto>> getBuyerChatRooms(@PathVariable Long buyerId) {
        return ResponseApi.ok(chatService.getOneToOneChatRoomsByBuyer(buyerId));
    }

    // 판매자 1:1 문의 채팅 목록 확인
    @GetMapping("/seller/one-to-one/{sellerId}")
    public ResponseApi<List<ChatItemDto>> getSellerChatRooms(@PathVariable Long sellerId) {
        return ResponseApi.ok(chatService.getOneToOneChatRoomsBySeller(sellerId));
    }

    // 판매자 1:1 문의 채팅 비활성화
    @PostMapping("/seller/{sellerId}/{chatId}")
    public ResponseApi<OneToOneChatResponseDto> setChatClosed(
            @PathVariable Long sellerId,
            @PathVariable Long chatId) {
        return ResponseApi.ok(chatService.setChatRoomClosed(sellerId, chatId));
    }

    // 그룹 채팅방 생성
    @PostMapping("/group")
    public ResponseApi<GroupChatResponseDto> createGroupChatRoom(
            @RequestBody @Valid GroupChatRequestDto requestDto)  {
        return ResponseApi.ok(chatService.createGroupChatRoom(requestDto));
    }

    // 참여 중인 그룹 채팅 목록 확인
    @GetMapping("/group/{memberId}")
    public ResponseApi<List<ChatItemDto>> getGroupChatRooms(@PathVariable Long memberId) {
        return ResponseApi.ok(chatService.getGroupChatRooms(memberId));
    }

}
