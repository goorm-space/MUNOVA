package com.space.munova.member.entity;

import com.space.munova.core.entity.BaseEntity;
import com.space.munova.member.dto.MemberRole;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "member_id")
    private Long id;

    private String username;

    private String password;

    @Enumerated(EnumType.STRING)
    MemberRole role;

    // 일반멤버 생성
    public static Member createMember(String username, String encodedPassword) {
        return Member.builder()
                .username(username)
                .password(encodedPassword)
                .role(MemberRole.USER)
                .build();
    }
}
