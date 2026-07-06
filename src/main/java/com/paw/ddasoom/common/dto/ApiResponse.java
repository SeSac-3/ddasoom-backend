package com.paw.ddasoom.common.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 정적 팩토리 메서드로만 생성하도록 봉인
public class ApiResponse<T> {

  private final String code;
  private final String message;
  private final T data;

  // 1. 데이터가 포함된 성공 응답
  public static <T> ApiResponse<T> success(T data) {
      return new ApiResponse<>("SUCCESS", "정상 처리 되었습니다.", data);
  }

  // 2. 데이터 + 커스텀 메시지 성공 응답 (예: "회원가입이 완료되었습니다.")
  public static <T> ApiResponse<T> success(String message, T data) {
      return new ApiResponse<>("SUCCESS", message, data);
  }

  // 3. 데이터가 없는 성공 응답 (예: 삭제 처리 등)
  public static ApiResponse<Void> success() {
      return new ApiResponse<>("SUCCESS", "정상 처리 되었습니다.", null);
  }

  // 4. 데이터 없이 커스텀 메시지만 있는 성공 응답 (예: "인증 코드가 발송되었습니다.")
  public static ApiResponse<Void> success(String message) {
      return new ApiResponse<>("SUCCESS", message, null);
  }

  // 5. 실패 응답 (GlobalExceptionHandler 전용 — 컨트롤러에서 직접 호출 금지)
  public static <T> ApiResponse<T> error(String code, String message) {
      return new ApiResponse<>(code, message, null);
  }

}
