package com.space.munova.chat.entity;


import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import com.space.munova.core.entity.BaseEntity;
import com.space.munova.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "chat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Chat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    private Member userId;

    @Column(nullable = false)
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
    public Chat(@NonNull String name, ChatStatus status, ChatType type, Member userId, Integer cur_participant, Integer max_participant) {
        this.name = name;
        this.status = status;
        this.type = type;
        this.userId = userId;
        this.cur_participant = cur_participant;
        this.max_participant = max_participant;
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
