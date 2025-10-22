package com.space.munova.chat.dto.group;

import com.space.munova.chat.entity.Chat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class GroupChatResponseDto {

    private Long chatId;    // 생성된 그룹 채팅방 아이디

    private Long memberId;  // 생성자 Id

    private String name;    // 그릅 채팅방 이름

    private LocalDateTime createdAt;    // 생성 시간

    public static GroupChatResponseDto to(Chat chat, Long memberId) {
        return new GroupChatResponseDto(
                chat.getId(), memberId, chat.getName(), chat.getCreatedAt()
        );
    }
}
