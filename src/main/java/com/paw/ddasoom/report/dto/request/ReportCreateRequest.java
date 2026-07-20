package com.paw.ddasoom.report.dto.request;

import com.paw.ddasoom.report.domain.ReportReason;
import com.paw.ddasoom.report.domain.ReportTargetType;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportCreateRequest {

  @NotNull(message = "신고 대상 유형은 필수입니다.")
  private ReportTargetType targetType;

  @NotNull(message = "신고 대상은 필수입니다.")
  private Long targetId;

  @NotNull(message = "신고 사유는 필수입니다.")
  private ReportReason reason;

  private String content; // ETC일 때 필수 사용

}
