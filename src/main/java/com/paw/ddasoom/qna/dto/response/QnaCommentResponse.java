package com.paw.ddasoom.qna.dto.response;

import java.time.LocalDateTime;

import com.paw.ddasoom.member.domain.Role;
import com.paw.ddasoom.qna.domain.QnaComment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QnaCommentResponse {

  private Long commentId;
  private String writerNickname;
  private Role writerRole;
  private String content;
  private LocalDateTime createdAt;

  public static QnaCommentResponse from(QnaComment comment) {
    return QnaCommentResponse.builder()
      .commentId(comment.getId())
      .writerNickname(comment.getMember().getNickname())
      .writerRole(comment.getMember().getRole())
      .content(comment.getContent())
      .createdAt(comment.getCreatedAt())
      .build();
  }

  
}
