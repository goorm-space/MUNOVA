package com.space.munova.chat.controller;


import com.space.munova.chat.dto.ChatItemDto;
import com.space.munova.chat.dto.group.*;
import com.space.munova.chat.dto.onetoone.OneToOneChatResponseDto;
import com.space.munova.chat.enums.ChatUserType;
import com.space.munova.chat.service.ChatRoomService;
import com.space.munova.core.config.ResponseApi;
import com.space.munova.member.dto.MemberRole;
import com.space.munova.security.jwt.JwtHelper;
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
        Long memberId = JwtHelper.getMemberId();
        return ResponseApi.ok(chatService.createOneToOneChatRoom(productId, memberId));
    }

    // 구매자의 1:1 문의 채팅 목록 확인(OPENED된 애들만 확인하도록)
    @GetMapping("/one-to-one")
    public ResponseApi<List<ChatItemDto>> getBuyerChatRooms() {
        Long memberId = JwtHelper.getMemberId();
        return ResponseApi.ok(chatService.getOneToOneChatRoomsByMember(ChatUserType.MEMBER, memberId));
    }

    // 판매자 1:1 문의 채팅 목록 확인(상태 상관 없음)
    @GetMapping("/seller/one-to-one")
    public ResponseApi<List<ChatItemDto>> getSellerChatRooms() {
        Long memberId = JwtHelper.getMemberId();
        return ResponseApi.ok(chatService.getOneToOneChatRoomsBySeller(memberId));
    }

    // 판매자 1:1 문의 채팅 비활성화
    @PatchMapping("/seller/{chatId}")
    public ResponseApi<ChatInfoResponseDto> setChatClosed(@PathVariable Long chatId) {
        Long memberId = JwtHelper.getMemberId();
        MemberRole role = JwtHelper.getMemberRole();
        return ResponseApi.ok(chatService.setChatRoomClosed(chatId, memberId, role));
    }

    // 그룹 채팅방 생성
    @PostMapping("/group")
    public ResponseApi<GroupChatInfoResponseDto> createGroupChatRoom(
            @RequestBody @Valid GroupChatRequestDto requestDto) {
        Long memberId = JwtHelper.getMemberId();
        return ResponseApi.ok(chatService.createGroupChatRoom(requestDto, memberId));
    }

    // 그룹 채팅방 조건 검색
    @GetMapping("/group/search")
    public ResponseApi<List<GroupChatDetailResponseDto>> searchGroupChatRooms(
            @RequestParam(required = false, name = "keyword") String keyword,
            @RequestParam(required = false, name = "tagIds") List<Long> tagIds,
            @RequestParam(defaultValue = "false", name = "isMine") Boolean isMine
    ) {
        Long memberId = JwtHelper.getMemberId();
        return ResponseApi.ok(chatService.searchGroupChatRooms(keyword, tagIds, isMine, memberId));
    }

    // 내 채팅방 목록 확인
    @GetMapping("/group/owner")
    public ResponseApi<List<GroupChatDetailResponseDto>> getMyGroupChatRooms() {
        Long memberId = JwtHelper.getMemberId();
        return ResponseApi.ok(chatService.getMyGroupChatRooms(memberId));
    }

    // 그룹 채팅방 정보 변경 (이름, 최대 참여자 수)
    @PatchMapping("/group/{chatId:\\d+}")
    public ResponseApi<ChatInfoResponseDto> updateGroupChatRoom(
            @PathVariable Long chatId, @RequestBody GroupChatUpdateRequestDto updateDto) {
        Long memberId = JwtHelper.getMemberId();
        return ResponseApi.ok(chatService.updateGroupChatInfo(chatId, updateDto, memberId));
    }

    // 그룹 채팅방 참여
    @PostMapping("/group/{chatId:\\d+}")
    public ResponseApi<Void> joinGroupChat(@PathVariable Long chatId) {
        Long memberId = JwtHelper.getMemberId();
        chatService.joinGroupChat(chatId, memberId);
        return ResponseApi.ok();
    }


    // 그룹채팅(Member) 채팅방 나가기
    @PostMapping("/group/leave/{chatId}")
    public ResponseApi<Void> leaveGroupChatRoom(@PathVariable Long chatId) {
        Long memberId = JwtHelper.getMemberId();
        chatService.leaveGroupChat(chatId, memberId);
        return ResponseApi.ok();
    }

    // 그룹채팅(Owner) 채팅방 비활성화
    @PatchMapping("/group/close/{chatId}")
    public ResponseApi<Void> closeGroupChat(@PathVariable Long chatId) {
        Long memberId = JwtHelper.getMemberId();
        chatService.closeGroupChat(chatId, memberId);
        return ResponseApi.ok();
    }

    // 그룹채팅(Owner) 채팅방 재활성화
    @PatchMapping("/group/open/{chatId}")
    public ResponseApi<Void> openGroupChat(@PathVariable Long chatId) {
        Long memberId = JwtHelper.getMemberId();
        chatService.openGroupChat(chatId, memberId);
        return ResponseApi.ok();
    }

    // 그룹채팅방 상세 조회 API
    @GetMapping("/group/{chatId}")
    public ResponseApi<GroupChatDetailResponseDto> getGroupChatDetail(@PathVariable Long chatId) {
        Long memberId = JwtHelper.getMemberId();
        return ResponseApi.ok(chatService.getGroupChatDetail(chatId, memberId));
    }


}
