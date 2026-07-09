package com.paw.ddasoom.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordChangeRequest {
  @NotBlank(message = "현재 비밀번호는 필수입니다.")
  private String currentPassword;   // 일치 여부만 판단 — 형식 검증 불필요

  @NotBlank(message = "새 비밀번호는 필수입니다.")
  @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$",
          message = "비밀번호는 대소문자, 숫자, 특수문자 포함 8자 이상이어야 합니다.")
  private String newPassword;       // SignupRequest와 동일 정규식 — 정책 일관
}
