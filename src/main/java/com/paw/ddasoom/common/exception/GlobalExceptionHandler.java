package com.paw.ddasoom.common.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.paw.ddasoom.common.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // BusinessException 계열 (AuthException, MemberException 등) — 정상 흐름의 거절
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        // 프론트 연동 디버깅용 한 줄 — 정상 거절이므로 스택트레이스는 남기지 않음
        log.warn("비즈니스 예외 - code: {}, path: {} {}",
                ex.getErrorCode().getCode(), request.getMethod(), request.getRequestURI());
        return ResponseEntity
                .status(ex.getErrorCode().getStatus())
                .body(ApiResponse.error(ex.getErrorCode().getCode(), ex.getErrorCode().getMessage()));
    }

    // @Valid 유효성 검사 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String errorMessage = fieldError != null ? fieldError.getDefaultMessage() : "입력값이 올바르지 않습니다.";

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("INVALID_INPUT", errorMessage));
    }

    // 요청 바디 파싱 실패 (Enum에 정의되지 않은 값, 깨진 JSON 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("요청 바디 파싱 실패 - path: {} {}", request.getMethod(), request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("INVALID_INPUT", "요청 형식이 올바르지 않습니다. 입력값을 확인해 주세요."));
    }

    // DB 무결성 제약 위반 (유니크 동시 충돌 등)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("DB 무결성 제약 위반 (동시 요청 충돌 추정) - path: {} {}",
                request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("DUPLICATE_CONFLICT", "이미 사용 중인 값이거나 요청이 겹쳤습니다. 다시 시도해 주세요."));
    }

    // 존재하지 않는 API 경로 → 404 (프론트의 URL 오타를 500으로 오인하지 않도록)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", "요청한 경로를 찾을 수 없습니다."));
    }

    // 허용되지 않은 HTTP 메서드 (POST 자리에 GET 등) → 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("METHOD_NOT_ALLOWED", "허용되지 않은 HTTP 메서드입니다."));
    }

    // 최후의 보루 — 여기 도달한 건 전부 "우리가 고쳐야 할 버그"
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllUnhandledException(
            Exception ex, HttpServletRequest request) {
        // 미처리 버그의 유일한 단서 — 스택트레이스 필수 (이 로그가 없으면 500의 원인을 알 수 없음)
        log.error("미처리 예외 발생 - path: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("GLOBAL_ERROR", "예상치 못한 서버 오류가 발생했습니다."));
    }
}
