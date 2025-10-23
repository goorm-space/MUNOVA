package com.space.munova.chat.entity;

import com.space.munova.chat.enums.MessageType;
import com.space.munova.core.entity.BaseEntity;
import com.space.munova.member.entity.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "message")
public class Message extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType type;   // TEXT, IMAGE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private Chat chatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Member userId;

    @Builder
    public Message(String content, MessageType type, Chat chatId, Member userId) {
        this.content = content;
        this.type = type;
        this.chatId = chatId;
        this.userId = userId;
    }

}
