package com.paw.ddasoom.board.dto.response;

import com.paw.ddasoom.board.domain.Post;
import com.paw.ddasoom.board.dto.projection.PostListProjection;
import lombok.Builder;
import lombok.Getter;
import org.jsoup.Jsoup;

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

    private static final int PREVIEW_LENGTH = 200;


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

    public static PostResponse from(PostListProjection projection, String thumbnailUrl) {
        return PostResponse.builder()
                .postId(projection.getPostId())
                .category(projection.getCategory())
                .title(projection.getTitle())
                .contentPreview(toPreview(projection.getContent()))   // ← 가공해서 담음
                .thumbnailUrl(thumbnailUrl)
                .author(AuthorResponse.from(projection.getAuthorId(), projection.getAuthorNickname()))
                .viewCount(projection.getViewCount())
                .commentCount(projection.getCommentCount())
                .createdAt(projection.getCreatedAt())
                .build();
    }

    // HTML 본문 → 태그·엔티티 제거한 순수 텍스트의 앞 PREVIEW_LENGTH자.
// 태그 제거를 '먼저' 하고 나서 잘라야 이미지가 앞에 와도 텍스트를 온전히 확보한다.
    private static String toPreview(String html) {
        if (html == null) return "";
        String text = Jsoup.parse(html).text();  // 태그 제거 + &nbsp; 등 엔티티 디코딩
        return text.length() > PREVIEW_LENGTH ? text.substring(0, PREVIEW_LENGTH) : text;
    }
}