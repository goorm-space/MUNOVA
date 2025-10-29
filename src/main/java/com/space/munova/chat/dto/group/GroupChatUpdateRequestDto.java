package com.space.munova.chat.dto.group;

public record GroupChatUpdateRequestDto(
        String name,

        Integer maxParticipants
) {
    public static GroupChatUpdateRequestDto of(String name, Integer maxParticipants) {
        return new GroupChatUpdateRequestDto(name, maxParticipants);
    }
}
