package com.space.munova.chat.service;

import com.space.munova.chat.dto.OneToOneChatRoomRequestDto;
import com.space.munova.chat.dto.OneToOneChatRoomResponseDto;
import org.springframework.stereotype.Service;


public interface ChatRoomService {

    OneToOneChatRoomResponseDto createOneToOneChatRoom(OneToOneChatRoomRequestDto requestChatRoomDto);
}
