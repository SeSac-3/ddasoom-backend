package com.paw.ddasoom.support.exception;

import org.springframework.http.HttpStatus;

import com.paw.ddasoom.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SupportErrorCode implements ErrorCode {

  NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "SUPPORT_001", "해당 공지사항을 찾을 수 없습니다."),
  FAQ_NOT_FOUND(HttpStatus.NOT_FOUND, "SUPPORT_002", "해당 FAQ를 찾을 수 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

}
