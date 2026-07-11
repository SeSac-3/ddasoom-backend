package com.paw.ddasoom.animal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnimalNicknameUpdateRequest(
  @NotBlank(message = "닉네임은 비어있을 수 없습니다.")
  @Size(min = 1, max = 20, message = "닉네임은 1자 이상 20자 이하로 입력해주세요.")
  String nickname
) {}
