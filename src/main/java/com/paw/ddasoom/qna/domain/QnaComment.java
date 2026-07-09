package com.paw.ddasoom.qna.domain;


import java.time.LocalDateTime;

import com.paw.ddasoom.common.util.BaseTimeEntity;
import com.paw.ddasoom.member.domain.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "qna_comment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaComment extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "qna_comment_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "qna_id", nullable = false)
  private Qna qna;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member; // 작성자 (유저/관리자 공용, role로 구분 예정)

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(columnDefinition = "DATETIME(6)")
  private LocalDateTime deletedAt;

  /* [QnA 댓글 최초 생성] */
  @Builder
  public QnaComment(Qna qna, Member member, String content) {
    this.qna = qna;
    this.member = member;
    this.content = content;
  }

  // moderation 전용 — 일반 delete/update 경로는 만들지 않음
  /* [QnA 댓글 논리 삭제 처리] */
  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }

  /* [QnA 댓글 삭제 상태 확인] */
  public boolean isDeleted() {
    return this.deletedAt != null;
  }
}
