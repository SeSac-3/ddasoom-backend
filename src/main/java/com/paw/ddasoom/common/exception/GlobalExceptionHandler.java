package com.paw.ddasoom.common.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.paw.ddasoom.common.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // BusinessException 으로 등록된 모든 예외 처리 핸들러 (AuthException, MemberException 등)
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
      return ResponseEntity
              .status(ex.getErrorCode().getStatus())
              .body(ApiResponse.error(ex.getErrorCode().getCode(), ex.getErrorCode().getMessage()));
  }

  // @Valid (정규식, Pattern, NotBlank 등) 유효성 검사 실패 예외 처리
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
      // 발생한 에러 중 첫 번째 에러 메시지를 추출 (예: "비밀번호는 대소문자... 8자 이상이어야 합니다.")
      FieldError fieldError = e.getBindingResult().getFieldError();
      String errorMessage = fieldError != null ? fieldError.getDefaultMessage() : "입력값이 올바르지 않습니다.";

      return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(ApiResponse.error("INVALID_INPUT", errorMessage));
  }

  /**
   * DB 무결성 제약 위반 (주로 유니크 제약 동시 위반).
   * 서비스의 existsBy 검사를 통과했으나 저장 직전 동시 요청에 선점된 레어 케이스.
   * 일반적인 중복은 서비스에서 AUTH_001/002로 먼저 잡히므로, 여기 도달 = 동시성 충돌.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
      log.warn("DB 무결성 제약 위반 (동시 요청 충돌 추정)", e);
      return ResponseEntity
              .status(HttpStatus.CONFLICT)
              .body(ApiResponse.error("DUPLICATE_CONFLICT", "이미 사용 중인 값이거나 요청이 겹쳤습니다. 다시 시도해 주세요."));
  }

  // 모든 처리되지 않은 예외를 위한 핸들러
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleAllUnhandledException(Exception ex) {
      return ResponseEntity
              .status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(ApiResponse.error("GLOBAL_ERROR", "예상치 못한 서버 오류가 발생했습니다."));
  }
}
