package com.space.munova.chat.service;

import com.space.munova.chat.dto.group.ChatInfoResponseDto;
import com.space.munova.chat.dto.group.GroupChatRequestDto;
import com.space.munova.chat.dto.ChatItemDto;
import com.space.munova.chat.dto.group.GroupChatUpdateRequestDto;
import com.space.munova.chat.dto.onetoone.OneToOneChatResponseDto;
import com.space.munova.chat.enums.ChatUserType;

import java.util.List;


public interface ChatRoomService {

    OneToOneChatResponseDto createOneToOneChatRoom(Long memberId, Long productId);

    List<ChatItemDto> getOneToOneChatRoomsByMember(Long memberId, ChatUserType chatUserType);

    ChatInfoResponseDto createGroupChatRoom(GroupChatRequestDto requestDto);

    List<ChatItemDto> getGroupChatRooms(Long memberId);

    ChatInfoResponseDto setChatRoomClosed(Long memberId, Long chatId);

    ChatInfoResponseDto updateGroupChatInfo(Long memberId, Long chatId, GroupChatUpdateRequestDto groupChatUpdateDto);

    void leaveGroupChat(Long memberId, Long chatId);

    void joinGroupChat(Long memberId, Long chatId);

    void closeGroupChat(Long memberId, Long chatId);
}
