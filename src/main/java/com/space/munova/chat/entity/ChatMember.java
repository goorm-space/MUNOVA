package com.space.munova.chat.entity;


import com.space.munova.chat.enums.ChatUserType;
import com.space.munova.member.entity.Member;
import com.space.munova.product.domain.product.Product;
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

    private ChatUserType type;

    public ChatMember(Chat chatId, Member memberId, ChatUserType type) {
        this.chatId = chatId;
        this.memberId = memberId;
        this.type = type;
    }
}
