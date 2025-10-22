package com.space.munova.chat.service;

import com.space.munova.chat.dto.group.GroupChatRequestDto;
import com.space.munova.chat.dto.group.GroupChatResponseDto;
import com.space.munova.chat.dto.ChatItemDto;
import com.space.munova.chat.dto.onetoone.OneToOneChatRequestDto;
import com.space.munova.chat.dto.onetoone.OneToOneChatResponseDto;

import java.util.List;


public interface ChatRoomService {

    OneToOneChatResponseDto createOneToOneChatRoom(Long memberId, Long productId);

    List<ChatItemDto> getOneToOneChatRoomsByBuyer(Long memberId);

    List<ChatItemDto> getOneToOneChatRoomsBySeller(Long memberId);

    GroupChatResponseDto createGroupChatRoom(GroupChatRequestDto requestDto);

    List<ChatItemDto> getGroupChatRooms(Long memberId);

    OneToOneChatResponseDto setChatRoomClosed(Long memberId, Long chatId);

}
