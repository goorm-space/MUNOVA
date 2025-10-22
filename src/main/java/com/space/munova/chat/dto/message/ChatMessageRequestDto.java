package com.space.munova.chat.dto.message;

import com.space.munova.chat.enums.MessageType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChatMessageRequestDto {

    private Long chatId;

    private Long senderId;

    private MessageType messageType;

    private String content;
}
