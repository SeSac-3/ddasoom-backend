package com.paw.ddasoom.board.dto.projection;

import com.paw.ddasoom.board.domain.BoardType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 마이페이지 "내가 쓴 댓글" 목록 전용 projection — API 응답 DTO 아님.
 * post를 JOIN해 원글 제목/보드타입을 평면화(N+1 방지) — 행 클릭 시 게시글 상세로 이동하기 위함.
 */
@Getter
@AllArgsConstructor
public class MyCommentListProjection {

    private final Long commentId;
    private final String content;
    private final LocalDateTime createdAt;
    private final Long postId;
    private final String postTitle;
    private final BoardType boardType;
}