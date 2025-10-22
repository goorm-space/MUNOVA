package com.space.munova.chat.service;

import com.space.munova.chat.dto.OneToOneChatItemDto;
import com.space.munova.chat.dto.OneToOneChatRequestDto;
import com.space.munova.chat.dto.OneToOneChatResponseDto;

import java.util.List;


public interface ChatRoomService {

    OneToOneChatResponseDto createOneToOneChatRoom(OneToOneChatRequestDto requestChatRoomDto);

    List<OneToOneChatItemDto> getOneToOneChatRoomsbyBuyer(Long userId);

    List<OneToOneChatItemDto> getOneToOneChatRoomsbySeller(Long userId);

}
