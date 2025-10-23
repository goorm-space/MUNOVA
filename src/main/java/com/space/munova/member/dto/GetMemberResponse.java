package com.space.munova.member.dto;

public record GetMemberResponse(
        String username,
        String address,
        MemberRole role
) {

    public static GetMemberResponse of(String username, String address, MemberRole role) {
        return new GetMemberResponse(username, address, role);
    }
}
