package org.example.owoonwan.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_INVALID_REQUEST", "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_NOT_FOUND", "리소스를 찾을 수 없습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    USER_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "USER_ALREADY_DELETED", "삭제된 사용자입니다."),

    NICKNAME_NOT_FOUND(HttpStatus.NOT_FOUND, "NICKNAME_NOT_FOUND", "닉네임을 찾을 수 없습니다."),
    NICKNAME_INACTIVE(HttpStatus.BAD_REQUEST, "NICKNAME_INACTIVE", "비활성화된 닉네임입니다."),
    NICKNAME_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "NICKNAME_ALREADY_ASSIGNED", "이미 할당된 닉네임입니다."),
    NICKNAME_ALREADY_FIXED(HttpStatus.CONFLICT, "NICKNAME_ALREADY_FIXED", "닉네임은 최초 1회만 선택할 수 있습니다."),
    NICKNAME_NOT_SELECTED(HttpStatus.BAD_REQUEST, "NICKNAME_NOT_SELECTED", "로그인을 위해 닉네임 선택이 필요합니다."),

    LOGIN_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "LOGIN_ID_ALREADY_EXISTS", "이미 사용 중인 로그인 아이디입니다."),
    LOGIN_ID_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "LOGIN_ID_INVALID_FORMAT", "로그인 아이디 형식이 올바르지 않습니다."),

    SESSION_NOT_FOUND(HttpStatus.UNAUTHORIZED, "SESSION_NOT_FOUND", "세션을 찾을 수 없습니다."),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED", "세션이 만료되었습니다."),
    SESSION_INACTIVE(HttpStatus.UNAUTHORIZED, "SESSION_INACTIVE", "비활성화된 세션입니다."),
    SESSION_LOCK_CONFLICT(HttpStatus.CONFLICT, "SESSION_LOCK_CONFLICT", "동시 로그인 요청이 감지되어 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
