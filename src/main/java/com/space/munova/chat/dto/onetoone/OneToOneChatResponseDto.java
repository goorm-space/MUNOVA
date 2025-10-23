package com.space.munova.chat.dto.onetoone;

import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@AllArgsConstructor
public class OneToOneChatResponseDto {

    private Long chatId;

    private Long sellerId;

    private Long buyerId;

    private String name;    // 채팅방 이름

    private LocalDateTime createdAt;

    private ChatStatus chatStatus;

    public static OneToOneChatResponseDto to(Chat chat, Member buyer, Member seller) {
        return OneToOneChatResponseDto.builder()
                .chatId(chat.getId())
                .sellerId(seller.getId())
                .buyerId(buyer.getId())
                .name(chat.getName())
                .createdAt(chat.getCreatedAt())
                .chatStatus(chat.getStatus())
                .build();
    }
}
