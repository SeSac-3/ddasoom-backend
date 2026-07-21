package com.paw.ddasoom.board.dto.response;

import com.paw.ddasoom.board.domain.BoardType;
import com.paw.ddasoom.board.dto.projection.AdminPostListProjection;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 게시글 목록 행 응답.
 *
 * <p>사용자용 {@link PostResponse} 대비:
 * <ul>
 *   <li>boardType 포함 — 전 보드를 한 화면에 섞어 보여주므로 필요.</li>
 *   <li>deletedAt 포함 — 상태 구분(활성/삭제됨)은 이 값으로 프론트가 뱃지 표시.
 *       (AdminMemberResponse가 탈퇴 여부를 deletedAt으로 내려주는 것과 동일 규격)</li>
 *   <li>contentPreview/thumbnailUrl 없음 — 제목 기준 모더레이션이라 불필요.</li>
 * </ul>
 */
@Getter
public class AdminPostResponse {

    private final Long postId;
    private final BoardType boardType;
    private final String category;
    private final String title;
    private final AuthorResponse author;
    private final int viewCount;
    private final int commentCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime deletedAt;   // null = 활성, 값 있음 = 삭제됨

    @Builder
    private AdminPostResponse(Long postId, BoardType boardType, String category, String title,
                              AuthorResponse author, int viewCount, int commentCount,
                              LocalDateTime createdAt, LocalDateTime deletedAt) {
        this.postId = postId;
        this.boardType = boardType;
        this.category = category;
        this.title = title;
        this.author = author;
        this.viewCount = viewCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    public static AdminPostResponse from(AdminPostListProjection projection) {
        return AdminPostResponse.builder()
                .postId(projection.getPostId())
                .boardType(projection.getBoardType())
                .category(projection.getCategory())
                .title(projection.getTitle())
                .author(AuthorResponse.from(projection.getAuthorId(), projection.getAuthorNickname()))
                .viewCount(projection.getViewCount())
                .commentCount(projection.getCommentCount())
                .createdAt(projection.getCreatedAt())
                .deletedAt(projection.getDeletedAt())
                .build();
    }
}