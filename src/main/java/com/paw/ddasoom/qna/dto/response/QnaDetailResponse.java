package com.paw.ddasoom.qna.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.paw.ddasoom.qna.domain.Qna;
import com.paw.ddasoom.qna.domain.QnaComment;
import com.paw.ddasoom.qna.domain.QnaStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QnaDetailResponse {

  private Long qnaId;
  private String questionerNickname;
  private String title;
  private String content;
  private QnaStatus status;
  private LocalDateTime createdAt;    // 문의 작성일
  private LocalDateTime answeredAt;   // 최신 답변일 (미답변 null)
  private LocalDateTime updatedAt;
  private List<QnaCommentResponse> comments;

  public static QnaDetailResponse from(Qna qna, List<QnaComment> comments) {
    return QnaDetailResponse.builder()
      .qnaId(qna.getId())
      .questionerNickname(qna.getQuestioner().getNickname())
      .title(qna.getTitle())
      .content(qna.getContent())
      .status(qna.getStatus())
      .createdAt(qna.getCreatedAt())
      .answeredAt(qna.getAnsweredAt())
      .updatedAt(qna.getUpdatedAt())
      .comments(comments.stream().map(QnaCommentResponse::from).toList())
      .build();

  }
  
}
