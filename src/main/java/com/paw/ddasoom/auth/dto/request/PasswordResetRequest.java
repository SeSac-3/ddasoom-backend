package com.paw.ddasoom.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 비밀번호 재설정 Request*/
@Getter
@NoArgsConstructor
public class PasswordResetRequest {

    @NotBlank(message = "토큰은 필수입니다.")
    private String token;

    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$",
            message = "비밀번호는 대소문자, 숫자, 특수문자 포함 8자 이상이어야 합니다.")
    private String newPassword;
}
