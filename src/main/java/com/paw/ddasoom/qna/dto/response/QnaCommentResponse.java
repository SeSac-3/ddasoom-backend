package com.paw.ddasoom.qna.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.paw.ddasoom.image.dto.response.ImageResponse;
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
  private List<ImageResponse> images;

  public static QnaCommentResponse from(QnaComment comment, List<ImageResponse> images) {
    return QnaCommentResponse.builder()
      .commentId(comment.getId())
      .writerNickname(comment.getMember().getNickname())
      .writerRole(comment.getMember().getRole())
      .content(comment.getContent())
      .createdAt(comment.getCreatedAt())
      .images(images)
      .build();
  }

  
}
