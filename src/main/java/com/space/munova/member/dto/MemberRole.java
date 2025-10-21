package com.space.munova.member.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberRole {

    USER("ROLE_USER", "일반 사용자"),
    SELLER("ROLE_SELLER", "판매자"),
    ADMIN("ROLE_ADMIN", "관리자");

    private final String key;
    private final String description;
}
