package com.paw.ddasoom.board.dto.response;

import com.paw.ddasoom.board.domain.BoardType;
import com.paw.ddasoom.board.domain.Post;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 게시글 목록 조회용 응답.
 * content(본문)는 미포함 — 리스트 하나에 본문 전체가 다 실리는 걸 방지.
 * 본문이 필요하면 PostDetailResponse를 사용.
 */
@Getter
public class PostResponse {

    private final Long postId;
    private final BoardType boardType;
    private final String category;
    private final String title;
    private final AuthorResponse author;
    private final int viewCount;
    private final int commentCount;
    private final LocalDateTime createdAt;

    @Builder
    private PostResponse(Long postId, BoardType boardType, String category, String title,
                         AuthorResponse author, int viewCount, int commentCount, LocalDateTime createdAt) {
        this.postId = postId;
        this.boardType = boardType;
        this.category = category;
        this.title = title;
        this.author = author;
        this.viewCount = viewCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
    }

    public static PostResponse from(Post post) {
        return PostResponse.builder()
                .postId(post.getId())
                .boardType(post.getBoardType())
                .category(post.getCategory())
                .title(post.getTitle())
                .author(AuthorResponse.from(post.getMember()))
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}