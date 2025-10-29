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

    public static ChatException chatClosedException(String... detailMessage) {
        return new ChatException("CHAT_02", "이미 종료된 채팅방입니다.", HttpStatus.CONFLICT, detailMessage);
    }

    public static ChatException unauthorizedParticipantException(String... detailMessage) {
        return new ChatException("CHAT_03", "채팅방에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN, detailMessage);
    }

    public static ChatException invalidOperationException(String... detailMessage) {
        return new ChatException("CHAT_04", "현재 참여 인원이 최대 정원보다 많아 정원 변경이 불가합니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static ChatException emptyChatNameException(String... detailMessage) {
        return new ChatException("CHAT_05", "채팅방 이름은 비워둘 수 없습니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static ChatException exceedMaxParticipantsException(String... detailMessage) {
        return new ChatException("CHAT_06", "채팅방 정원 초과", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static ChatException cannotDecrementParticipantsException(String... detailMessage) {
        return new ChatException("CHAT_07", "참여자 수 0 이하로 감소 불가", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static ChatException alreadyJoinedException(String... detailMessage) {
        return new ChatException("CHAT_08", "이미 참여중인 채팅방입니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static ChatException duplicateChatNameException(String... detailMessage) {
        return new ChatException("CHAT_09", "이미 존재하는 채팅방 이름입니다.", HttpStatus.CONFLICT, detailMessage);
    }

    public static ChatException invalidChatRoomException(String... detailMessage) {
        return new ChatException("CHAT_10", "유효하지 않은 채팅방입니다.", HttpStatus.NOT_FOUND, detailMessage);
    }

    public static ChatException notAllowedToCreateChatWithSelf(String... detailMessage) {
        return new ChatException("CHAT_11", "자신이 등록한 상품에는 문의할 수 없습니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }

// ======================

    public static ChatException cannotFindMemberException(String... detailMessage) {
        return new ChatException("AUTH_01", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND, detailMessage);
    }

    public static ChatException cannotFindProductException(String... detailMessage) {
        return new ChatException("PROD_01", "상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND, detailMessage);
    }

    public static ChatException unauthorizedAccessException(String... detailMessage) {
        return new ChatException("AUTH_02", "권한이 없습니다.", HttpStatus.FORBIDDEN, detailMessage);
    }
}
