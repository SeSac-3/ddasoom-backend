package com.paw.ddasoom.auth.exception;

import org.springframework.http.HttpStatus;

import com.paw.ddasoom.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode{

  /**---------------------------- 
     이메일 인증 관련 에러코드 
  ----------------------------- */
  EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_001", "이미 사용 중인 이메일입니다."),
  NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_002", "이미 사용 중인 닉네임입니다."),
  EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "AUTH_003", "이메일 인증이 완료되지 않았습니다."),
  INVALID_AUTH_CODE(HttpStatus.BAD_REQUEST, "AUTH_004", "인증 번호가 일치하지 않거나 만료되었습니다."),
  MAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_005", "메일 전송에 실패했습니다."),

  /**---------------------------- 
     로그인 관련 에러코드 
  ----------------------------- */
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_101", "이메일 또는 비밀번호가 일치하지 않습니다."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_102", "인증이 필요합니다. 다시 로그인해 주세요."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_103", "접근 권한이 없습니다."),
  INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_104", "토큰 재발급에 실패했습니다."),

  /**---------------------------- 
     회원가입 관련 에러코드 
  ----------------------------- */
  SOCIAL_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_106", "이미 해당 이메일로 가입된 계정이 있습니다. 기존 계정으로 로그인해 주세요."),
  SOCIAL_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "AUTH_107", "소셜 계정의 이메일 제공에 동의해 주세요.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}
