package com.space.munova.chat.exception;

import com.space.munova.core.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class ChatException extends BaseException {

    public ChatException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }

    public static ChatException cannotFindChatException(String... detailMessage) {
        return new ChatException("CHAT_01", "채팅방을 찾을 수 없습니다.", HttpStatus.NOT_FOUND, detailMessage);
    }


// ======================

    public static ChatException cannotFindMemberException(String... detailMessage) {
        return new ChatException("AUTH_01", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND, detailMessage);
    }

    public static ChatException cannotFindProductException(String... detailMessage) {
        return new ChatException("PROD_01", "상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND, detailMessage);
    }
}
