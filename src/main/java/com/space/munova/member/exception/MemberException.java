package com.space.munova.member.exception;

import com.space.munova.core.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class MemberException extends BaseException {

    public MemberException(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }

    public static MemberException invalidMemberException(String... detailMessage) {
        return new MemberException("MEMBER_01", "사용자 정보가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED, detailMessage);
    }
}
