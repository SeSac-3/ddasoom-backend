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
  INVALID_FOSTER_STATUS(HttpStatus.BAD_REQUEST, "FOSTER_004", "변경할 수 없는 임시보호 상태입니다."),
  INVALID_FOSTER_SEARCH_CONDITION(HttpStatus.BAD_REQUEST,"FOSTER_005","단일 상태 조회와 묶음 조회는 동시에 사용할 수 없습니다."),
  INVALID_FOSTER_DATE_RANGE(HttpStatus.BAD_REQUEST,"FOSTER_006","조회 시작일은 종료일보다 늦을 수 없습니다."),
  INVALID_FOSTER_STATUS_TRANSITION(HttpStatus.BAD_REQUEST,"FOSTER_007","허용되지 않은 임시보호 상태 변경입니다."),
  INVALID_FOSTER_PERIOD(HttpStatus.BAD_REQUEST,"FOSTER_008","임시보호 기간 정보가 올바르지 않습니다."),
  DUPLICATE_FOSTER_APPLICATION(HttpStatus.BAD_REQUEST,"FOSTER_009","이미 임시보호 신청 중입니다. 거절되었거나 삭제된 신청만 다시 신청할 수 있습니다."),
  ALREADY_FOSTERED_ANIMAL(HttpStatus.BAD_REQUEST,"FOSTER_010","이미 임시보호 중인 동물은 신청할 수 없습니다."),
  INVALID_FOSTER_UPDATE_STATUS(HttpStatus.BAD_REQUEST,"FOSTER_011","신청 대기 또는 신청 거절 상태에서만 임시보호 신청을 수정할 수 있습니다."),
  INVALID_FOSTER_DELETE_STATUS(HttpStatus.BAD_REQUEST,"FOSTER_012","신청 대기 또는 신청 거절 상태에서만 임시보호 신청을 삭제할 수 있습니다."),
  INCOMPLETE_FOSTER_ADMIN_UPDATE(HttpStatus.BAD_REQUEST,"FOSTER_013","관리자 수정 시 기존 답변과 임시보호 일정은 모두 포함해서 보내야 합니다."),
  REQUIRED_FOSTER_SCHEDULE(HttpStatus.BAD_REQUEST,"FOSTER_014","현재 임시보호 상태에 필요한 일정 정보가 누락되었습니다.");


  private final HttpStatus status;
  private final String code;
  private final String message;

}
