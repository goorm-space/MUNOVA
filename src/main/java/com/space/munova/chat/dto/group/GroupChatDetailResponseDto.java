package com.space.munova.chat.dto.group;

import com.space.munova.chat.enums.ChatStatus;

import java.time.LocalDateTime;
import java.util.List;

public record GroupChatDetailResponseDto(
        Long chatId,
        String name,
        Integer maxParticipant,
        Integer currentParticipant,
        ChatStatus status,
        LocalDateTime createdAt,
        List<String> productCategoryList,
        List<MemberInfoDto> memberList
) {
    public static GroupChatDetailResponseDto of(
            Long chatId,
            String name,
            Integer maxParticipant,
            Integer currentParticipant,
            ChatStatus status,
            LocalDateTime createdAt,
            List<String> productCategoryList,
            List<MemberInfoDto> memberList
    ) {
        return new GroupChatDetailResponseDto(
                chatId,
                name,
                maxParticipant,
                currentParticipant,
                status,
                createdAt,
                productCategoryList,
                memberList
        );
    }
}
