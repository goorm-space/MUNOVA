package com.space.munova.chat.entity;


import com.space.munova.chat.dto.group.GroupChatUpdateRequestDto;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import com.space.munova.chat.exception.ChatException;
import com.space.munova.core.entity.BaseEntity;
import com.space.munova.member.dto.MemberRole;
import com.space.munova.product.domain.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
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
    private ChatStatus status;  // OPENED, CLOSED

    @Enumerated(EnumType.STRING)
    private ChatType type;  // GROUP, ONE_ON_ONE

    private Integer curParticipant;

    private Integer maxParticipant;

    private String lastMessageContent;

    private LocalDateTime lastMessageTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product productId;

    @Builder.Default
    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatTag> chatTags = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "chatId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMember> chatMembers = new ArrayList<>();

    public static Chat createChat(String name, ChatStatus status, ChatType type, Product product, Integer curParticipant, Integer maxParticipant){
        return Chat.builder()
                .name(name)
                .status(status)
                .type(type)
                .productId(product)
                .curParticipant(curParticipant)
                .maxParticipant(maxParticipant).build();
    }

    public void modifyLastMessageContent(String lastMessageContent, LocalDateTime lastMessageTime) {
        if (lastMessageContent.length() > 20) {
            this.lastMessageContent = lastMessageContent.substring(0, 20) + "...";
        } else {
            this.lastMessageContent = lastMessageContent;
        }
        this.lastMessageTime = lastMessageTime;
    }

    public void updateChatStatus(ChatStatus status) {
        if (status == this.status) {
            throw ChatException.chatClosedException("chatStatusClosed");
        }
        this.status = status;
    }

    public void updateInfo(GroupChatUpdateRequestDto groupChatUpdateDto) {
        if (groupChatUpdateDto.maxParticipants() < curParticipant) {
            throw ChatException.invalidOperationException("Max participants : " + maxParticipant + "\n" +
                    "Requested : " + maxParticipant);
        }
        if (groupChatUpdateDto.name() == null || groupChatUpdateDto.name().isBlank()) return;
        this.maxParticipant = groupChatUpdateDto.maxParticipants();
        this.name = groupChatUpdateDto.name();
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

    public void oneToOneChatCloseBySeller(MemberRole role) {
        if(role != MemberRole.SELLER){
            throw ChatException.unauthorizedAccessException("판매자만 채팅방을 닫을 수 있습니다.");
        }
        if(this.status == ChatStatus.CLOSED){
            throw ChatException.chatClosedException("chatStatusClosed");
        }
        this.status = ChatStatus.CLOSED;
    }

}
