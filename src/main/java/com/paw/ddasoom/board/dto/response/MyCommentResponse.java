package com.paw.ddasoom.board.dto.response;

import com.paw.ddasoom.board.domain.BoardType;
import com.paw.ddasoom.board.dto.projection.MyCommentListProjection;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 마이페이지 "내가 쓴 댓글" 목록 응답 — 댓글 내용 + 원글 정보(제목/경로 조립용 boardType).
 * author 미포함(전부 본인 댓글).
 */
@Getter
public class MyCommentResponse {

    private final Long commentId;
    private final String content;
    private final LocalDateTime createdAt;
    private final Long postId;
    private final String postTitle;
    private final BoardType boardType;      // 프론트가 /board/{slug}/{postId} 경로를 만들 때 사용

    @Builder
    private MyCommentResponse(Long commentId, String content, LocalDateTime createdAt,
                              Long postId, String postTitle, BoardType boardType) {
        this.commentId = commentId;
        this.content = content;
        this.createdAt = createdAt;
        this.postId = postId;
        this.postTitle = postTitle;
        this.boardType = boardType;
    }

    public static MyCommentResponse from(MyCommentListProjection projection) {
        return MyCommentResponse.builder()
                .commentId(projection.getCommentId())
                .content(projection.getContent())
                .createdAt(projection.getCreatedAt())
                .postId(projection.getPostId())
                .postTitle(projection.getPostTitle())
                .boardType(projection.getBoardType())
                .build();
    }
}