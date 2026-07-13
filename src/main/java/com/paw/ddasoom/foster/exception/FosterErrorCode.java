package com.paw.ddasoom.foster.exception;

import org.springframework.http.HttpStatus;

import com.paw.ddasoom.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FosterErrorCode implements ErrorCode {

  FOSTER_NOT_FOUND(HttpStatus.NOT_FOUND, "FOSTER_001", "해당 임시 보호 신청을 찾을 수 없습니다."),
  FOSTER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "FOSTER_002", "해당 임시보호 신청에 접근할 권한이 없습니다."),
  ALREADY_DELETED_FOSTER(HttpStatus.BAD_REQUEST, "FOSTER_003", "이미 삭제된 임시보호 신청입니다."),
  INVALID_FOSTER_STATUS(HttpStatus.BAD_REQUEST, "FOSTER_004", "변경할 수 없는 임시보호 상태입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

}
