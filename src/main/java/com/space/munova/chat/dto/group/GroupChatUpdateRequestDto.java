package com.space.munova.chat.dto.group;

import lombok.Getter;
import lombok.Setter;

@Getter
public class GroupChatUpdateRequestDto {
    private Long chatId;

    public String name;

    private Integer maxParticipants;
}
