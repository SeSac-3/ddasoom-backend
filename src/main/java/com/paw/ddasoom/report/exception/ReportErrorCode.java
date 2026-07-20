package com.paw.ddasoom.report.exception;

import org.springframework.http.HttpStatus;

import com.paw.ddasoom.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCode {

  REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT_001", "해당 신고를 찾을 수 없습니다."),
  REPORT_DUPLICATE(HttpStatus.CONFLICT, "REPORT_002", "이미 신고한 대상입니다."),
  REPORT_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "REPORT_003", "이미 처리된 신고입니다."),
  REPORT_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "REPORT_004", "기타 사유는 상세 내용을 입력해야 합니다."),
  REPORT_SELF(HttpStatus.BAD_REQUEST, "REPORT_005", "자기 자신은 신고할 수 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
