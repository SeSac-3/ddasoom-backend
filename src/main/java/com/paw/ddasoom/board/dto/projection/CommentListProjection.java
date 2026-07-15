package com.paw.ddasoom.board.dto.projection;

import lombok.Getter;

import java.time.LocalDateTime;


/**
 * 댓글 목록 조회용 Projection — member 조인 결과를 평면 값으로 수령.
 * JPQL SELECT NEW 대상이므로 생성자 파라미터 순서가 쿼리와 일치해야 함.
 */
@Getter
public class CommentListProjection {

    private final Long commentId;
    private final Long memberId;
    private final String nickname;
    private final String content;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public CommentListProjection(Long commentId, Long memberId, String nickname,
                                 String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.commentId = commentId;
        this.memberId = memberId;
        this.nickname = nickname;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
