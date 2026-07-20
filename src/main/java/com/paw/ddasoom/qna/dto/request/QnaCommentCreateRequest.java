package com.paw.ddasoom.qna.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QnaCommentCreateRequest {

  @NotBlank(message = "내용은 필수입니다.")
  private String content;

  private List<Long> imageIds;

}
