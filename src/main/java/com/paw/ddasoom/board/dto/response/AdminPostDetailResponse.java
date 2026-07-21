package com.paw.ddasoom.board.dto.response;

import com.paw.ddasoom.board.domain.BoardType;
import com.paw.ddasoom.board.domain.Post;
import com.paw.ddasoom.image.dto.response.ImageResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 게시글 상세 응답.
 *
 * <p>사용자용 {@link PostDetailResponse}와 거의 같되 {@code deletedAt}을 추가로 내려준다 —
 * 관리자는 이미 삭제된 글의 상세도 열어볼 수 있어야 하고(강제삭제 확인·소명 판단),
 * 이때 삭제 상태를 화면에 표시하기 위함이다.
 *
 * <p>⚠️ 조회수를 올리지 않는다 — 관리자 열람은 집계 대상이 아니므로 사용자 상세 조회 경로와 분리한다.
 * (본문 sanitize는 저장 시점에 이미 완료되어 있으므로 그대로 내려도 안전 — 프론트는 SafeHtmlViewer로 이중 방어)
 */
@Getter
public class AdminPostDetailResponse {

    private final Long postId;
    private final BoardType boardType;
    private final String category;
    private final String title;
    private final String content;
    private final AuthorResponse author;
    private final List<ImageResponse> images;
    private final int viewCount;
    private final int commentCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;   // null = 활성, 값 있음 = 삭제됨

    @Builder
    private AdminPostDetailResponse(Long postId, BoardType boardType, String category, String title,
                                    String content, AuthorResponse author, List<ImageResponse> images,
                                    int viewCount, int commentCount,
                                    LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.postId = postId;
        this.boardType = boardType;
        this.category = category;
        this.title = title;
        this.content = content;
        this.author = author;
        this.images = images;
        this.viewCount = viewCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static AdminPostDetailResponse from(Post post, List<ImageResponse> images) {
        return AdminPostDetailResponse.builder()
                .postId(post.getId())
                .boardType(post.getBoardType())
                .category(post.getCategory())
                .title(post.getTitle())
                .content(post.getContent())
                .author(AuthorResponse.from(post.getMember()))
                .images(images)
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .deletedAt(post.getDeletedAt())
                .build();
    }
}