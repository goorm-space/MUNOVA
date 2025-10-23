package com.space.munova.chat.dto.group;

import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.enums.ChatStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatInfoResponseDto {

    private Long chatId;    // 생성된 그룹 채팅방 아이디

//    private Long memberId;  // 생성자 Id

    private String name;    // 그릅 채팅방 이름

    private int maxParticipant;

    private ChatStatus status;

    private LocalDateTime createdAt;    // 생성 시간

    public static ChatInfoResponseDto to(Chat chat) {
        return new ChatInfoResponseDto(
                chat.getId(), chat.getName(), chat.getMaxParticipant(), chat.getStatus(), chat.getCreatedAt()
        );
    }
}
