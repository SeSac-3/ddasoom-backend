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

/**
 * 게시글(post) 엔티티.
 *
 * <p>커뮤니티 게시판의 글 한 건을 표현한다. 보드 종류는 {@link BoardType}, 세부 분류는 {@code category}로 나눈다.
 * 생성/수정 시각은 {@link BaseTimeEntity}가 관리하며, 삭제는 물리 삭제가 아닌 {@code deletedAt} 기반 soft delete다.
 *
 * <p>설계 원칙:
 * <ul>
 *   <li>{@code @Setter} 전면 금지 — 모든 상태 변경은 아래 리치 도메인 메서드로만 수행한다.</li>
 *   <li>{@code @NoArgsConstructor(PROTECTED)} + {@code @Builder} 조합 — 무분별한 생성을 막고 빌더로만 생성.</li>
 *   <li>{@code view_count}/{@code comment_count}는 캐시 컬럼이며, 전용 증감 메서드로만 갱신한다.</li>
 * </ul>
 */
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

    /**
     * 게시글 생성용 빌더 생성자. 감사 컬럼(created_at 등)과 카운트(0)/deletedAt(null)은 기본값으로 시작한다.
     *
     * @param member    작성자
     * @param boardType 보드 타입
     * @param category  세부 카테고리
     * @param title     제목
     * @param content   본문 (sanitize된 HTML)
     */
    @Builder
    private Post(Member member, BoardType boardType, String category, String title, String content) {
        this.member = member;
        this.boardType = boardType;
        this.category = category;
        this.title = title;
        this.content = content;
    }

    // ===== 리치 도메인 메서드 (setter 금지 — 상태 변경은 아래 메서드로만) =====

    /**
     * 게시글 내용을 수정한다. 작성자/조회수/댓글수/생성일 등 나머지 상태는 유지된다.
     *
     * @param boardType 변경할 보드 타입
     * @param category  변경할 세부 카테고리
     * @param title     변경할 제목
     * @param content   변경할 본문 (sanitize된 HTML)
     */
    public void update(BoardType boardType, String category, String title, String content) {
        this.boardType = boardType;
        this.category = category;
        this.title = title;
        this.content = content;
    }

    /** 조회수를 1 증가시킨다. (중복 집계 방지는 서비스 계층이 담당) */
    public void increaseViewCount() {
        this.viewCount++;
    }

    /** 댓글수 캐시를 1 증가시킨다. (댓글 생성 시 서비스가 호출) */
    public void increaseCommentCount() {
        this.commentCount++;
    }

    /** 댓글수 캐시를 1 감소시킨다. (댓글 삭제 시 서비스가 호출) */
    public void decreaseCommentCount() {
        // 캐시 컬럼이 음수가 되지 않도록 안전망 (DB INT UNSIGNED와 일관)
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    /** 게시글을 soft delete한다 — deletedAt에 현재 시각을 세팅(물리 삭제 아님). */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 삭제 여부를 반환한다.
     *
     * @return deletedAt이 세팅되어 있으면 {@code true}(삭제됨), NULL이면 {@code false}(활성)
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}