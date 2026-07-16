package com.paw.ddasoom.support.dto.request;

import java.util.List;

import com.paw.ddasoom.support.domain.FaqCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FaqUpdateRequest {

  @NotBlank(message = "카테고리는 필수입니다.")
  private FaqCategory category;

  @NotBlank(message = "질문은 필수입니다.")
  @Size(max = 255, message = "질문은 255자를 초과할 수 없습니다.")
  private String question;

  @NotBlank(message = "답변은 필수입니다.")
  private String answer;

  private List<Long> imageIds;
}
