package com.paw.ddasoom.foster.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FosterUpdateRequest {

  @NotBlank(message = "나이 입력은 필수입니다.")
  @Size(max = 10, message = "나이 입력은 10자 이하로 입력해주세요.")
  @Pattern(regexp = "^[0-9]+$", message = "숫자만 입력할 수 있습니다.")
  private String age;

  @NotBlank(message = "직업 입력은 필수입니다.")
  @Pattern(regexp = "^[가-힣a-zA-Z0-9 ]+$", message = "직업은 한글, 영문, 숫자만 입력할 수 있습니다.")
  @Size(max = 30, message = "직업 입력은 30자 이하로 입력해주세요.")
  private String job;

  @Size(max = 1000, message = "신청 메세지는 1000자 이하로 입력 가능합니다.")
  private String message;
}
