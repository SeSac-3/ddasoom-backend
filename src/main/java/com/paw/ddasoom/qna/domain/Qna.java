package com.paw.ddasoom.qna.domain;

import java.time.LocalDateTime;

import com.paw.ddasoom.common.util.BaseTimeEntity;
import com.paw.ddasoom.member.domain.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "qna")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Qna extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qna_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questioner_id", nullable = false)
    private Member questioner;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /* [최적화: SSOT] QnA 생성 시 기본 상태는 항상 '답변 대기'로 선언 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QnaStatus status = QnaStatus.PENDING;

    @Column(nullable = false)
    private Boolean isVisible=true;

    @Column(columnDefinition = "DATETIME(6)")
    private LocalDateTime answeredAt;

    @Column(columnDefinition = "DATETIME(6)")
    private LocalDateTime deletedAt;

    @Builder
    public Qna(Member questioner, String title, String content) {
        this.questioner = questioner;
        this.title = title;
        this.content = content;
    }

    /* [QnA 답변 작성 시 상태 변경 + 답변 일시 갱신] */
    public void markAnswered() {
        this.status = QnaStatus.ANSWERED;
        this.answeredAt = LocalDateTime.now(); // 매 답변마다 최신화
    }

    /* [유저 재질문 코멘트 추가 → 답변 대기] */
    public void markPending() {
        this.status = QnaStatus.PENDING;
    }

    /* [QnA 노출 여부 변경] */
    public void changeVisibility(boolean isVisible) {
        this.isVisible = isVisible;
    }

    /* [QnA 논리 삭제 처리] */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /* [QnA 삭제 상태 확인] */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
  }