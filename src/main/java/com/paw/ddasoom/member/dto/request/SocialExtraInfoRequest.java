package com.paw.ddasoom.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SocialExtraInfoRequest {
  @NotBlank(message = "이름은 필수입니다.")
  @Pattern(regexp = "^[가-힣]+$|^[a-zA-Z\\s]+$", message = "이름은 한글 또는 영문만 입력할 수 있습니다.")
  private String name;

  @NotBlank(message = "닉네임은 필수입니다.")
  @Pattern(
        regexp = "^[a-zA-Z0-9가-힣]{2,10}$", 
        message = "닉네임은 2~10자의 한글, 영문, 숫자만 가능합니다."
  )
  private String nickname;

  @NotBlank(message = "전화번호는 필수입니다.")
  @Pattern(
        regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", 
        message = "휴대폰 번호는 01로 시작하며 '-' 없이 10~11자리여야 합니다."
  )
  private String tel;
}
