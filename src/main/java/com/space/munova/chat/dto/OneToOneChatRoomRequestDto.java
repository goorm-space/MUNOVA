package com.space.munova.chat.dto;

import com.space.munova.chat.enums.ChatType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class OneToOneChatRoomRequestDto {

    private Long buyerId;

    private Long sellerId;

    private String name;    // 채팅방 이름

    private ChatType type;  // GROUP, ONE_ON_ONE

}
