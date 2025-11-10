package com.space.munova.member.entity;

import com.space.munova.core.entity.BaseEntity;
import com.space.munova.member.dto.MemberRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.util.StringUtils;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = "username")
})
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    private String username;

    private String password;

    private String address;

    @Enumerated(EnumType.STRING)
    MemberRole role;

    // 일반멤버 생성
    public static Member createMember(String username, String encodedPassword, String address) {
        return Member.builder()
                .username(username)
                .password(encodedPassword)
                .address(address)
                .role(MemberRole.USER)
                .build();
    }

    // 맴버 업데이트
    public void updateMember(String username, String address, MemberRole role) {
        if (StringUtils.hasText(username)) {
            this.username = username;
        }
        if (StringUtils.hasText(address)) {
            this.address = address;
        }
        if (role != null) {
            this.role = role;
        }
    }

}
