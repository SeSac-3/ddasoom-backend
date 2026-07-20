package com.paw.ddasoom.qna.dto.request;

import java.util.List;

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

  private List<Long> imageIds; // optional — 미리 업로드된 이미지 id 목록

}
