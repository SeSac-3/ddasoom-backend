package com.paw.ddasoom.board.dto.projection;

import com.paw.ddasoom.board.domain.BoardType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 마이페이지 "내가 쓴 글" 목록 전용 projection — API 응답 DTO 아님.
 *
 * PostListProjection과의 차이:
 * - boardType 포함: 마이페이지는 여러 보드의 글이 섞여 나오므로, 프론트가 상세 페이지 경로
 *   (/board/{slug}/{postId})를 만들려면 boardType이 필요함 (일반 목록은 보드별 페이지라 불필요했음)
 * - author 미포함: 전부 본인 글이므로 닉네임 조인 불필요
 */
@Getter
@AllArgsConstructor
public class MyPostListProjection {

    private final Long postId;
    private final BoardType boardType;
    private final String category;
    private final String title;
    private final String content;          // 앞 200자 SUBSTRING (PostListProjection과 동일 정책)
    private final int viewCount;
    private final int commentCount;
    private final LocalDateTime createdAt;
}