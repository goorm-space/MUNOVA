package com.space.munova.chat.dto.group;

import com.space.munova.chat.enums.ChatUserType;

public record MemberInfoDto(
        Long memberId,
        String name,
        ChatUserType chatUserType

) {
    public static MemberInfoDto of(Long memberId, String name, ChatUserType chatUserType) {
        return new MemberInfoDto(memberId, name, chatUserType);
    }
}
