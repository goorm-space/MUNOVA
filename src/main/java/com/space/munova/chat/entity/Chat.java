package com.space.munova.chat.entity;


import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import com.space.munova.chat.exception.ChatException;
import com.space.munova.core.entity.BaseEntity;
import com.space.munova.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chat")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Chat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private ChatStatus status;

    @Enumerated(EnumType.STRING)
    private ChatType type;

    private Integer curParticipant;

    private Integer maxParticipant;

    private String lastMessageContent;

    private LocalDateTime lastMessageTime;

    @Builder
    public Chat(@NonNull String name, ChatStatus status, ChatType type, Integer curParticipant, Integer maxParticipant) {
        this.name = name;
        this.status = status;
        this.type = type;
//        this.userId = userId;
        this.curParticipant = curParticipant;
        this.maxParticipant = maxParticipant;
    }

    public void modifyLastMessageContent(String lastMessageContent, LocalDateTime lastMessageTime) {
        if(lastMessageContent.length() > 20){
            this.lastMessageContent = lastMessageContent.substring(0, 20) + "...";
        } else{
            this.lastMessageContent = lastMessageContent;
        }
        this.lastMessageTime = lastMessageTime;
    }

    public void updateStatus(ChatStatus status) {
        this.status = status;
    }

    public void updateMaxParticipant(Integer newMaxParticipant) {
        if(newMaxParticipant > maxParticipant){
            throw ChatException.invalidOperationException("Max participants : " + maxParticipant + "\n" +
                    "Requested : " + maxParticipant);
        }
        this.maxParticipant = newMaxParticipant;
    }

    public void updateName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw ChatException.emptyChatNameException();
        }
        this.name = newName;
    }

    public void incrementParticipant() {
        if (curParticipant >= maxParticipant) {
            throw ChatException.exceedMaxParticipantsException(
                    "Current participants: " + curParticipant + "\n" + "Max participants: " + maxParticipant
            );
        }
        this.curParticipant += 1;
    }

    public void decrementParticipant() {
        if (curParticipant <= 0) {
            throw ChatException.cannotDecrementParticipantsException();
        }
        this.curParticipant -= 1;
    }

}
