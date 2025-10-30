package com.space.munova.s3;

import com.space.munova.core.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class S3Exception extends BaseException {

    public S3Exception(String code, String message, HttpStatusCode statusCode, String... detailMessage) {
        super(code, message, statusCode, detailMessage);
    }

    public static S3Exception throwUnsupportedFileTypeException(String... detailMessage) {
        return new S3Exception("S3_01", "허용되지 않은 파일 형식입니다. PDF, JPEG, PNG 파일만 업로드 가능합니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }

    public static S3Exception fileNotFoundException(String... detailMessage) {
        return new S3Exception("S3_02", "파일이 존재하지 않습니다.", HttpStatus.BAD_REQUEST, detailMessage);
    }
}
