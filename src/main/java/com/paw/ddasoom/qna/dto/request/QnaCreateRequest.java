package com.paw.ddasoom.qna.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QnaCreateRequest {

  @NotBlank(message = "제목은 필수입니다.")
  @Size(max = 255, message = "제목은 255글자를 초과할 수 없습니다.")
  private String title;

  @NotBlank(message = "내용은 필수입니다.")
  private String content;

}
