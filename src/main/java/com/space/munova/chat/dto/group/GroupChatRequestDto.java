package com.space.munova.chat.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record GroupChatRequestDto(
        @NotBlank(message = "채팅방 이름은 필수입니다.")
        String chatName,
        @NotNull(message = "최대 참여 인원 설정은 필수입니다.")
        Integer maxParticipants
) {
    public static GroupChatRequestDto of(String chatName, Integer maxParticipants) {
        return new GroupChatRequestDto(chatName, maxParticipants);
    }
}
