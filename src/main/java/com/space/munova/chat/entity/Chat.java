package com.space.munova.chat.entity;


import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import com.space.munova.core.entity.BaseEntity;
import com.space.munova.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Chat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    private Member userId;

    private String name;

    @Enumerated(EnumType.STRING)
    private ChatStatus status;

    @Enumerated(EnumType.STRING)
    private ChatType type;

    private Integer cur_participant;

    private Integer max_participant;

    private String lastMessageContent;

    private LocalDateTime lastMessageTime;

    @Builder
    public Chat(String name, ChatStatus status, ChatType type, Member userId) {
        this.name = name;
        this.status = status;
        this.type = type;
        this.userId = userId;
    }

    public void modifyLastMessageContent(String lastMessageContent, LocalDateTime lastMessageTime) {
        if(lastMessageContent.length() > 20){
            this.lastMessageContent = lastMessageContent.substring(0, 20) + "...";
        } else{
            this.lastMessageContent = lastMessageContent;
        }
        this.lastMessageTime = lastMessageTime;
    }

}
