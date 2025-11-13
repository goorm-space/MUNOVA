package com.space.munova.chat.entity;


import com.space.munova.chat.enums.ChatUserType;
import com.space.munova.member.entity.Member;
import com.space.munova.product.domain.Product;
import jakarta.persistence.*;
import lombok.*;

@Builder
@Getter
@Entity
@Table(name = "chat_member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private Chat chatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member memberId;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "product_id")
//    private Product productId;

    @Enumerated(EnumType.STRING)
    private ChatUserType chatMemberType;    // 채팅방 권한 : ADMIN, MEMBER, OWNER

    private String name;    // 채팅 참여자 이름

    public static ChatMember createChatMember(Chat chat, Member member, ChatUserType type, String name) {
        return ChatMember.builder()
                .chatId(chat)
                .memberId(member)
                .chatMemberType(type)
                .name(name)
                .build();
    }

    public Long getMemberIdValue() {
        return memberId != null ? memberId.getId() : null;
    }
}
