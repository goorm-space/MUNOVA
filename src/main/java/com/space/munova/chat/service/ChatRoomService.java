package com.space.munova.chat.service;

import com.space.munova.chat.dto.ChatItemDto;
import com.space.munova.chat.dto.group.ChatInfoResponseDto;
import com.space.munova.chat.dto.group.GroupChatRequestDto;
import com.space.munova.chat.dto.group.GroupChatUpdateRequestDto;
import com.space.munova.chat.dto.onetoone.OneToOneChatResponseDto;
import com.space.munova.chat.enums.ChatUserType;

import java.util.List;


public interface ChatRoomService {

    OneToOneChatResponseDto createOneToOneChatRoom(Long productId);

    List<ChatItemDto> getOneToOneChatRoomsByMember(ChatUserType chatUserType);

    ChatInfoResponseDto createGroupChatRoom(GroupChatRequestDto requestDto);

    List<ChatItemDto> getGroupChatRooms();

    ChatInfoResponseDto setChatRoomClosed(Long chatId);

    ChatInfoResponseDto updateGroupChatInfo(Long chatId, GroupChatUpdateRequestDto groupChatUpdateDto);

    void leaveGroupChat(Long chatId);

    void joinGroupChat(Long chatId);

    void closeGroupChat(Long chatId);
}
