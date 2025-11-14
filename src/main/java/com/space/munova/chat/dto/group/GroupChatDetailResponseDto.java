package com.space.munova.chat.dto.group;

import com.space.munova.chat.entity.Chat;
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
    public static GroupChatDetailResponseDto of(Chat chat){
        List<String> tags = chat.getChatTags() == null ? List.of() :
                chat.getChatTags().stream().map(c -> c.getCategoryType().getDescription()).toList();

        List<MemberInfoDto> memberInfoDtos = chat.getChatMembers() == null ? List.of() :
                chat.getChatMembers().stream().map(MemberInfoDto::of).toList();

        return new GroupChatDetailResponseDto(
                chat.getId(),
                chat.getName(),
                chat.getMaxParticipant(),
                chat.getCurParticipant(),
                chat.getStatus(),
                chat.getCreatedAt(),
                tags,
                memberInfoDtos);
    }
}
