package com.space.munova.chat.enums;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum MessageType {
    SEND, SUBSCRIBE, UNSUBSCRIBE

}
