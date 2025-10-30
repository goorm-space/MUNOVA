package com.space.munova.member.dto;

public record GetMemberResponse(
        Long memberId,
        String username,
        String address,
        MemberRole role
) {

    public static GetMemberResponse of(Long memberId, String username, String address, MemberRole role) {
        return new GetMemberResponse(memberId, username, address, role);
    }
}
