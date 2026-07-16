package com.paw.ddasoom.qna.dto.response;

import java.time.LocalDateTime;

import com.paw.ddasoom.qna.domain.Qna;
import com.paw.ddasoom.qna.domain.QnaStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QnaSummaryResponse {

  private Long qnaId;
  private String questionerNickname;
  private String title;
  private QnaStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime answeredAt;

  public static QnaSummaryResponse from (Qna qna) {
    return QnaSummaryResponse.builder()
      .qnaId(qna.getId())
      .questionerNickname(qna.getQuestioner().getNickname())
      .title(qna.getTitle())
      .status(qna.getStatus())
      .createdAt(qna.getCreatedAt())
      .answeredAt(qna.getAnsweredAt())
      .build();
  }

}
