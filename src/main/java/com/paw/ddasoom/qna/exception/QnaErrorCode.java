package com.paw.ddasoom.qna.exception;

import org.springframework.http.HttpStatus;

import com.paw.ddasoom.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QnaErrorCode implements ErrorCode {

  QNA_NOT_FOUND(HttpStatus.NOT_FOUND, "QNA_001", "해당 문의를 찾을 수 없습니다."),
  QNA_ACCESS_DENIED(HttpStatus.FORBIDDEN, "QNA_002", "본인의 문의만 접근할 수 있습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
  
}
