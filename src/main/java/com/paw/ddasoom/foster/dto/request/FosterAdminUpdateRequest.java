package com.paw.ddasoom.foster.dto.request;

import java.time.LocalDateTime;

import com.paw.ddasoom.foster.domain.FosterStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FosterAdminUpdateRequest {

  @Size(max = 1000, message = "관리자 답변은 1000자 이하로 입력 가능합니다.")
  private String answer;

  @NotNull(message = "임시보호 상태는 필수입니다.")
  private FosterStatus status;

  private LocalDateTime fosterStartAt;
  private LocalDateTime fosterEndAt;
  private LocalDateTime fosterExtendAt;
  private LocalDateTime fosterCompleteAt;
  
}
