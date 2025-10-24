package com.space.munova.chat.dto.message;

import com.space.munova.chat.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChatMessageRequestDto {

    private Long senderId;

    private MessageType messageType;

    private String content;
}
