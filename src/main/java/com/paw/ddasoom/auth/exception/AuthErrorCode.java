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
   AUTH_CODE_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "AUTH_006",
          "인증 메일은 1분에 한 번만 발송할 수 있습니다. 잠시 후 다시 시도해 주세요."),
  // AUTH_006(이메일 단위 쿨다운)과 별도 코드인 이유: 프론트가 AUTH_006을 "1분에 한 번" 문구로
  // 분기 중이라, 성격이 다른 IP 단위 제한(시간당)을 같은 코드에 실으면 안내가 틀려짐
  AUTH_CODE_IP_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "AUTH_007",
          "인증 메일 요청이 너무 많습니다. 1시간 후 다시 시도해 주세요."),
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
  SOCIAL_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "AUTH_107", "소셜 계정의 이메일 제공에 동의해 주세요."),

   /**---------------------------- 
     회원가입 관련 에러코드 
  ----------------------------- */
   INVALID_RESET_TOKEN(HttpStatus.BAD_REQUEST, "AUTH_108", "비밀번호 재설정 링크가 만료되었거나 유효하지 않습니다."),

   /**---------------------------- 
     회원 탈퇴 관련 에러코드 
  ----------------------------- */
   WITHDRAWN_MEMBER(HttpStatus.FORBIDDEN, "AUTH_109",
          "탈퇴 처리된 계정입니다. 계정 복구를 원하시면 1:1 문의를 이용해 주세요.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}
