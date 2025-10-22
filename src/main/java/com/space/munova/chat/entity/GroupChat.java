package com.space.munova.chat.entity;


import com.space.munova.chat.enums.ChatUserType;
import com.space.munova.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private Chat chatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Member memberId;

    private ChatUserType type;

    public GroupChat(Chat chatId, Member memberId) {
        this.chatId = chatId;
        this.memberId = memberId;
    }
}
