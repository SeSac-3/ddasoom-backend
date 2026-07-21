package com.paw.ddasoom.board.dto.projection;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 댓글 목록 조회용 projection — member 조인 결과를 평면 값으로 수령.
 *
 * <p>사용자용 {@link CommentListProjection}과의 차이는 {@code deletedAt} 포함 하나뿐:
 * 관리자 화면은 이미 삭제된 댓글도 목록에 노출하고 "삭제됨" 뱃지로 구분해야 하므로,
 * 사용자 목록 쿼리처럼 {@code deletedAt IS NULL}로 걸러내지 않고 상태를 그대로 내려준다.
 *
 * <p>JPQL SELECT NEW 대상이므로 생성자 파라미터 순서가 쿼리와 일치해야 함.
 */
@Getter
public class AdminCommentListProjection {

    private final Long commentId;
    private final Long memberId;
    private final String nickname;
    private final String content;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;   // NULL = 활성, 값 있음 = 삭제됨

    public AdminCommentListProjection(Long commentId, Long memberId, String nickname,
                                      String content, LocalDateTime createdAt,
                                      LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.commentId = commentId;
        this.memberId = memberId;
        this.nickname = nickname;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }
}