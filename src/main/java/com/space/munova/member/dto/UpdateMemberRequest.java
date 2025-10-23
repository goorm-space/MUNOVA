package com.space.munova.member.dto;

import com.space.munova.core.utils.ValidEnum;

public record UpdateMemberRequest(
        String username,
        String address,
        @ValidEnum(enumClass = MemberRole.class)
        String role
) {
}
