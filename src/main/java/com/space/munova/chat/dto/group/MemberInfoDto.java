package com.space.munova.chat.dto.group;

public record MemberInfoDto(
        Long memberId,
        String name
) {
    public static MemberInfoDto of(Long memberId, String name) {
        return new MemberInfoDto(memberId, name);
    }
}
