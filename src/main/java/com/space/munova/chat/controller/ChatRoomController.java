package com.space.munova.chat.controller;


import com.space.munova.chat.dto.group.ChatInfoResponseDto;
import com.space.munova.chat.dto.group.GroupChatUpdateRequestDto;
import com.space.munova.chat.dto.group.GroupChatRequestDto;
import com.space.munova.chat.dto.ChatItemDto;
import com.space.munova.chat.dto.onetoone.OneToOneChatResponseDto;
import com.space.munova.chat.enums.ChatUserType;
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
        return ResponseApi.ok(chatService.getOneToOneChatRoomsByMember(buyerId, ChatUserType.OWNER));
    }

    // 판매자 1:1 문의 채팅 목록 확인
    @GetMapping("/seller/one-to-one/{sellerId}")
    public ResponseApi<List<ChatItemDto>> getSellerChatRooms(@PathVariable Long sellerId) {
        return ResponseApi.ok(chatService.getOneToOneChatRoomsByMember(sellerId, ChatUserType.MEMBER));
    }

    // 판매자 1:1 문의 채팅 비활성화
    @PatchMapping("/seller/{sellerId}/{chatId}")
    public ResponseApi<ChatInfoResponseDto> setChatClosed(
            @PathVariable Long sellerId,
            @PathVariable Long chatId) {
        return ResponseApi.ok(chatService.setChatRoomClosed(sellerId, chatId));
    }

    // 그룹 채팅방 생성
    @PostMapping("/group")
    public ResponseApi<ChatInfoResponseDto> createGroupChatRoom(
            @RequestBody @Valid GroupChatRequestDto requestDto)  {
        return ResponseApi.ok(chatService.createGroupChatRoom(requestDto));
    }

    // 참여 중인 그룹 채팅 목록 확인
    @GetMapping("/group/{memberId}")
    public ResponseApi<List<ChatItemDto>> getGroupChatRooms(@PathVariable Long memberId) {
        return ResponseApi.ok(chatService.getGroupChatRooms(memberId));
    }

    // 그룹 채팅방 정보 변경 (이름, 최대 참여자 수)
    @PatchMapping("/group/{memberId}/{chatId}")
    public ResponseApi<ChatInfoResponseDto> updateGroupChatRoom(
            @PathVariable Long memberId, @PathVariable Long chatId, @RequestBody GroupChatUpdateRequestDto updateDto){
        return ResponseApi.ok(chatService.updateGroupChatInfo(memberId, chatId, updateDto));
    }

    // 그룹 채팅방 참여
    @PostMapping("/group/{memberId}/{chatId}")
    public ResponseApi<Void> joinGroupChat(
            @PathVariable Long memberId, @PathVariable Long chatId){
        chatService.joinGroupChat(memberId, chatId);
        return ResponseApi.ok();
    }


    // 그룹채팅(Member) 채팅방 나가기
    @DeleteMapping("/group/{memberId}/{chatId}")
    public ResponseApi<Void> leaveGroupChatRoom(
            @PathVariable Long memberId, @PathVariable Long chatId){
        chatService.leaveGroupChat(memberId, chatId);
        return ResponseApi.ok();
    }

//    // 그룹채팅(Owner) 채팅방 닫기
//    @PatchMapping("/group/{memberId}/{chatId}/close")
//    public ResponseApi<Void> closeGroupChat(
//            @PathVariable Long memberId, @PathVariable Long chatId){
//        chatService.closeGroupChat(memberId, chatId);
//    }

    // 그룹채팅(Owner) 인원 강퇴



}
