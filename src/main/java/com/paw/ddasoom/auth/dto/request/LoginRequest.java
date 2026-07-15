package com.paw.ddasoom.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

  @NotBlank(message = "이메일은 필수입니다.")
  @Email(message = "이메일 형식이 올바르지 않습니다.")
  private String email;

  @NotBlank(message = "비밀번호는 필수입니다.")
  @Size(max = 64, message = "비밀번호는 64자 이하여야 합니다.")
  private String password;   // 로그인은 형식 검증 불필요 — 일치 여부만 판단
}
