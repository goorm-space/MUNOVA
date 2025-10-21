package com.space.munova.chat.dto;

import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.User;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class OneToOneChatRoomResponseDto {

    private Long ChatId;

    private Long sellerId;

    private Long buyerId;

    private ChatType chatType;

    private ChatStatus chatStatus;

    private LocalDateTime createdAt;

    public static OneToOneChatRoomResponseDto to(Chat chat, User buyer, User seller) {
        return OneToOneChatRoomResponseDto.builder()
                .ChatId(chat.getId())
                .chatType(chat.getType())
                .chatStatus(chat.getStatus())
                .createdAt(chat.getCreatedAt())
                .sellerId(seller.getId())
                .buyerId(buyer.getId())
                .build();
    }
}
