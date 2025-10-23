package com.space.munova.chat.dto.onetoone;

import com.space.munova.chat.enums.ChatStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ChatInfoUpdateResponseDto {

    private String name;

    private Integer maxParticipants;

    private ChatStatus chatStatus;

}
