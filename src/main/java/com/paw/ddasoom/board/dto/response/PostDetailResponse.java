package com.paw.ddasoom.board.dto.response;

import com.paw.ddasoom.board.domain.BoardType;
import com.paw.ddasoom.board.domain.Post;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 게시글 상세 조회용 응답.
 * 댓글 목록은 미포함 — 댓글은 GET /api/posts/{postId}/comments 별도 API로 페이징 조회.
 * commentCount만 참고용으로 내려줌 (캐시 컬럼).
 */
@Getter
public class PostDetailResponse {

    private final Long postId;
    private final BoardType boardType;
    private final String category;
    private final String title;
    private final String content;
    private final AuthorResponse author;
    private final int viewCount;
    private final int commentCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    @Builder
    private PostDetailResponse(Long postId, BoardType boardType, String category, String title, String content,
                               AuthorResponse author, int viewCount, int commentCount,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.postId = postId;
        this.boardType = boardType;
        this.category = category;
        this.title = title;
        this.content = content;
        this.author = author;
        this.viewCount = viewCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PostDetailResponse from(Post post) {
        return PostDetailResponse.builder()
                .postId(post.getId())
                .boardType(post.getBoardType())
                .category(post.getCategory())
                .title(post.getTitle())
                .content(post.getContent())
                .author(AuthorResponse.from(post.getMember()))
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}