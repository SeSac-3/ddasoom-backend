package com.paw.ddasoom.support.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoticePinReorderRequest {

  @NotNull(message = "noticeIds는 필수입니다. (전체 해제 시 빈 배열 전송 가능)")
  private List<Long> noticeIds;

}
