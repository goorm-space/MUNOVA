package com.space.munova.chat.entity;


import com.space.munova.chat.enums.ChatUserType;
import com.space.munova.member.entity.Member;
import com.space.munova.product.domain.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product productId;

    @Enumerated(EnumType.STRING)
    private ChatUserType chatMemberType;    // 채팅방 권한 : ADMIN, MEMBER, OWNER

    private String name;

    public ChatMember(Chat chatId, Member memberId, ChatUserType type, Product product, String name) {
        this.chatId = chatId;
        this.memberId = memberId;
        this.chatMemberType = type;
        this.productId = product;
        this.name = name;
    }

    public ChatMember(Chat chatId, Member memberId, ChatUserType type, String name) {
        this.chatId = chatId;
        this.memberId = memberId;
        this.chatMemberType = type;
        this.name = name;
    }
}
