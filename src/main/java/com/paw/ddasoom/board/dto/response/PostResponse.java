package com.paw.ddasoom.board.dto.response;

import com.paw.ddasoom.board.domain.Post;
import com.paw.ddasoom.board.dto.projection.PostListProjection;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 게시글 목록 조회용 응답 (카드 UI).
 * content 전체는 미포함 — contentPreview(앞 200자)만 제공, 말줄임 처리는 프론트 CSS 책임.
 * boardType 미포함 — 게시판별로 호출 엔드포인트/컨텍스트가 이미 분리되어 있어 불필요.
 */
@Getter
public class PostResponse {

    private final Long postId;
    private final String category;
    private final String title;
    private final String contentPreview;
    private final String thumbnailUrl;      // 썸네일 미지정 시 null — 프론트가 기본 이미지 처리
    private final AuthorResponse author;
    private final int viewCount;
    private final int commentCount;
    private final LocalDateTime createdAt;

    @Builder
    private PostResponse(Long postId, String category, String title,
                         String contentPreview, String thumbnailUrl, AuthorResponse author,
                         int viewCount, int commentCount, LocalDateTime createdAt) {
        this.postId = postId;
        this.category = category;
        this.title = title;
        this.contentPreview = contentPreview;
        this.thumbnailUrl = thumbnailUrl;
        this.author = author;
        this.viewCount = viewCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
    }

    /** 목록 조회 전용 — projection + Service에서 배치 조회한 썸네일 URL 조합 */
    public static PostResponse from(PostListProjection projection, String thumbnailUrl) {
        return PostResponse.builder()
                .postId(projection.getPostId())
                .category(projection.getCategory())
                .title(projection.getTitle())
                .contentPreview(projection.getContentPreview())
                .thumbnailUrl(thumbnailUrl)
                .author(AuthorResponse.from(projection.getAuthorId(), projection.getAuthorNickname()))
                .viewCount(projection.getViewCount())
                .commentCount(projection.getCommentCount())
                .createdAt(projection.getCreatedAt())
                .build();
    }
}