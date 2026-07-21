package com.paw.ddasoom.board.domain;

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

import java.time.LocalDateTime;

/**
 * 게시글 댓글(post_comment) 엔티티.
 *
 * <p>게시글({@link Post})에 달린 댓글 한 건을 표현한다. {@code parentComment} 자기참조로 대댓글 구조를 지원하며,
 * 삭제는 물리 삭제가 아닌 {@code deletedAt} 기반 soft delete다. 생성/수정 시각은 {@link BaseTimeEntity}가 관리한다.
 *
 * <p>설계 원칙:
 * <ul>
 *   <li>{@code @Setter} 금지 — 모든 상태 변경은 아래 리치 도메인 메서드로만 수행한다.</li>
 *   <li>{@code @NoArgsConstructor(PROTECTED)} + {@code @Builder} 조합으로만 생성한다.</li>
 *   <li>댓글 수는 {@link Post}의 {@code comment_count} 캐시 컬럼으로 관리되며, 증감은 서비스가 담당한다.</li>
 * </ul>
 */
@Entity
@Table(name = "post_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /** 작성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 부모 댓글 (자기참조) — NULL = 일반 댓글, 값 있음 = 대댓글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private PostComment parentComment;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /** NULL = 활성 (soft delete) */
    @Column(name = "deleted_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime deletedAt;

    /**
     * 댓글 생성용 빌더 생성자. {@code deletedAt}은 null(활성)로 시작한다.
     *
     * @param post          댓글이 달릴 게시글
     * @param member        작성자
     * @param parentComment 부모 댓글 — 일반 댓글이면 {@code null}, 대댓글이면 부모 댓글
     * @param content       댓글 본문
     */
    @Builder
    private PostComment(Post post, Member member, PostComment parentComment, String content) {
        this.post = post;
        this.member = member;
        this.parentComment = parentComment;
        this.content = content;
    }

    // ===== 리치 도메인 메서드 =====

    /**
     * 댓글 본문을 수정한다.
     *
     * @param content 변경할 본문
     */
    public void updateContent(String content) {
        this.content = content;
    }

    /** 댓글을 soft delete한다 — deletedAt에 현재 시각을 세팅(물리 삭제 아님). */
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

    /**
     * 대댓글 여부를 반환한다.
     *
     * @return 부모 댓글이 있으면 {@code true}(대댓글), 없으면 {@code false}(일반 댓글)
     */
    public boolean isReply() {
        return this.parentComment != null;
    }
}