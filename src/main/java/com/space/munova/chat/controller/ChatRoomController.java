package com.space.munova.chat.controller;


import com.space.munova.chat.dto.ChatItemDto;
import com.space.munova.chat.dto.group.*;
import com.space.munova.chat.dto.onetoone.OneToOneChatResponseDto;
import com.space.munova.chat.enums.ChatUserType;
import com.space.munova.chat.service.ChatRoomService;
import com.space.munova.core.config.ResponseApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatRoomController {

    private final ChatRoomService chatService;

    // 문의하기 -> 일반 유저 1:1 채팅방 생성
    @PostMapping("/one-to-one/{productId}")
    public ResponseApi<OneToOneChatResponseDto> createOneToOneChatRoom(
            @PathVariable Long productId) {
        return ResponseApi.ok(chatService.createOneToOneChatRoom(productId));
    }

    // 구매자의 1:1 문의 채팅 목록 확인
    @GetMapping("/one-to-one")
    public ResponseApi<List<ChatItemDto>> getBuyerChatRooms() {
        return ResponseApi.ok(chatService.getOneToOneChatRoomsByMember(ChatUserType.MEMBER));
    }

    // 판매자 1:1 문의 채팅 목록 확인
    @GetMapping("/seller/one-to-one")
    public ResponseApi<List<ChatItemDto>> getSellerChatRooms() {
        return ResponseApi.ok(chatService.getOneToOneChatRoomsByMember(ChatUserType.OWNER));
    }

    // 판매자 1:1 문의 채팅 비활성화
    @PatchMapping("/seller/{chatId}")
    public ResponseApi<ChatInfoResponseDto> setChatClosed(@PathVariable Long chatId) {
        return ResponseApi.ok(chatService.setChatRoomClosed(chatId));
    }

    // 그룹 채팅방 생성
    @PostMapping("/group")
    public ResponseApi<GroupChatInfoResponseDto> createGroupChatRoom(
            @RequestBody @Valid GroupChatRequestDto requestDto) {
        return ResponseApi.ok(chatService.createGroupChatRoom(requestDto));
    }

    // 그룹 채팅방 조건 검색
    @GetMapping("/group/search")
    public ResponseApi<List<GroupChatInfoResponseDto>> searchGroupChatRooms(
            @RequestParam(required = false, name = "keyword") String keyword,
            @RequestParam(required = false, name = "tagIds") List<Long> tagIds,
            @RequestParam(defaultValue = "false", name = "isMine") Boolean isMine
    ) {
        log.info("keyword: {}, tagIds: {}", keyword, tagIds);

        return ResponseApi.ok(chatService.searchGroupChatRooms(keyword, tagIds, isMine));
    }

    // 참여 중인 그룹 채팅 목록 확인
    @GetMapping("/group")
    public ResponseApi<List<ChatItemDto>> getGroupChatRooms() {
        return ResponseApi.ok(chatService.getGroupChatRooms());
    }

    // 전체 그룹 채팅 목록 확인
    @GetMapping("/group/all")
    public ResponseApi<List<ChatItemDto>> getAllGroupChatRooms() {
        return ResponseApi.ok(chatService.getAllGroupChatRooms());
    }

    // 그룹 채팅방 정보 변경 (이름, 최대 참여자 수)
    @PatchMapping("/group/{chatId:\\d+}")
    public ResponseApi<ChatInfoResponseDto> updateGroupChatRoom(
            @PathVariable Long chatId, @RequestBody GroupChatUpdateRequestDto updateDto) {
        return ResponseApi.ok(chatService.updateGroupChatInfo(chatId, updateDto));
    }

    // 그룹 채팅방 참여
    @PostMapping("/group/{chatId:\\d+}")
    public ResponseApi<Void> joinGroupChat(@PathVariable Long chatId) {
        chatService.joinGroupChat(chatId);
        return ResponseApi.ok();
    }


    // 그룹채팅(Member) 채팅방 나가기
    @PostMapping("/group/leave/{chatId}")
    public ResponseApi<Void> leaveGroupChatRoom(@PathVariable Long chatId) {
        chatService.leaveGroupChat(chatId);
        return ResponseApi.ok();
    }

    // 그룹채팅(Owner) 채팅방 닫기
    @PatchMapping("/group/close/{chatId}")
    public ResponseApi<Void> closeGroupChat(@PathVariable Long chatId) {
        chatService.closeGroupChat(chatId);
        return ResponseApi.ok();
    }

    // 그룹채팅방 상세 조회 API
    @GetMapping("/group/{chatId}")
    public ResponseApi<GroupChatDetailResponseDto> getGroupChatDetail(@PathVariable Long chatId) {
        return ResponseApi.ok(chatService.getGroupChatDetail(chatId));
    }


}
