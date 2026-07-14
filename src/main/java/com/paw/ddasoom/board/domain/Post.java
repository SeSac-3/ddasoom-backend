package com.paw.ddasoom.board.domain;

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

import java.time.LocalDateTime;

@Entity
@Table(name = "post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    /** 작성자 (단방향: community → member, 컨벤션의 auth → member 허용 패턴과 동일) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", length = 30, nullable = false)
    private BoardType boardType;

    /** 보드 내 세부 카테고리 (예: 예방접종) — 종(강아지/고양이)은 boardType으로 분리됨. 값 목록 확정 전이라 String 유지 */
    @Column(name = "category", length = 50, nullable = false)
    private String category;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 조회수 (증가는 increaseViewCount()로만) */
    @Column(name = "view_count", nullable = false)
    private int viewCount;

    /** 댓글수 캐시 컬럼 — 댓글 생성/삭제 시 서비스가 도메인 메서드로 동기화 */
    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    /** NULL = 활성 (soft delete) */
    @Column(name = "deleted_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime deletedAt;

    @Builder
    private Post(Member member, BoardType boardType, String category, String title, String content) {
        this.member = member;
        this.boardType = boardType;
        this.category = category;
        this.title = title;
        this.content = content;
    }

    // ===== 리치 도메인 메서드 (setter 금지 — 상태 변경은 아래 메서드로만) =====

    public void update(BoardType boardType, String category, String title, String content) {
        this.boardType = boardType;
        this.category = category;
        this.title = title;
        this.content = content;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void increaseCommentCount() {
        this.commentCount++;
    }

    public void decreaseCommentCount() {
        // 캐시 컬럼이 음수가 되지 않도록 안전망 (DB INT UNSIGNED와 일관)
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}