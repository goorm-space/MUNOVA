package com.space.munova.chat.dto.group;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.*;

import java.util.List;


public record GroupChatRequestDto(
        @NotBlank(message = "채팅방 이름은 필수입니다.")
        String chatName,

        @NotNull(message = "최대 참여 인원 설정은 필수입니다.")
        @Min(value = 1, message = "최소 1명 이상이어야 합니다.")
        @Max(value = 1000, message = "최대 참여 인원은 1000명을 초과할 수 없습니다.")
        Integer maxParticipants,
        
        @Nullable
        @Size(max = 4, message = "채팅방 태그는 최대 4개까지 선택 가능합니다.")
        List<Long> productCategoryId
) {
    public static GroupChatRequestDto of(String chatName, Integer maxParticipants, List<Long> productCategoryId) {
        return new GroupChatRequestDto(chatName, maxParticipants, productCategoryId);
    }
}
