package com.paw.ddasoom.member.exception;

import org.springframework.http.HttpStatus;

import com.paw.ddasoom.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode{

  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_001", "해당 회원을 찾을 수 없습니다."),
  ALREADY_USER_ROLE(HttpStatus.BAD_REQUEST, "MEMBER_002", "이미 추가 정보 입력이 완료된 회원입니다."),
  ALREADY_DELETED_MEMBER(HttpStatus.BAD_REQUEST, "MEMBER_003", "이미 탈퇴 처리된 회원입니다."),
  PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "MEMBER_004", "현재 비밀번호가 일치하지 않습니다."),
  SOCIAL_MEMBER_HAS_NO_PASSWORD(HttpStatus.BAD_REQUEST, "MEMBER_005", "소셜 로그인 회원은 비밀번호를 변경할 수 없습니다."),
  NOT_DELETED_MEMBER(HttpStatus.BAD_REQUEST, "MEMBER_006", "탈퇴 상태가 아닌 회원입니다."),
  CANNOT_WITHDRAW_ADMIN(HttpStatus.BAD_REQUEST, "MEMBER_007", "관리자 계정은 강제 탈퇴할 수 없습니다."),
  CANNOT_CHANGE_ADMIN_STATUS(HttpStatus.BAD_REQUEST, "MEMBER_008", "관리자 계정의 상태는 변경할 수 없습니다.");

  
  // 실제 상태(필드)는 Enum이 가집니다.
    private final HttpStatus status;
    private final String code;
    private final String message;
}
