package com.paw.ddasoom.board.dto.response;

import com.paw.ddasoom.board.domain.BoardType;
import com.paw.ddasoom.board.dto.projection.MyPostListProjection;
import lombok.Builder;
import lombok.Getter;
import org.jsoup.Jsoup;

import java.time.LocalDateTime;

/**
 * 마이페이지 "내가 쓴 글" 목록 응답.
 * PostResponse 대비 boardType 포함(상세 경로 조립용) / author 미포함(전부 본인 글).
 */
@Getter
public class MyPostResponse {

    private final Long postId;
    private final BoardType boardType;      // 프론트가 /board/{slug}/{postId} 경로를 만들 때 사용
    private final String category;
    private final String title;
    private final String contentPreview;
    private final String thumbnailUrl;      // 썸네일 미지정 시 null — 프론트가 기본 아이콘 처리
    private final int viewCount;
    private final int commentCount;
    private final LocalDateTime createdAt;

    private static final int PREVIEW_LENGTH = 200;

    @Builder
    private MyPostResponse(Long postId, BoardType boardType, String category, String title,
                           String contentPreview, String thumbnailUrl,
                           int viewCount, int commentCount, LocalDateTime createdAt) {
        this.postId = postId;
        this.boardType = boardType;
        this.category = category;
        this.title = title;
        this.contentPreview = contentPreview;
        this.thumbnailUrl = thumbnailUrl;
        this.viewCount = viewCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
    }

    public static MyPostResponse from(MyPostListProjection projection, String thumbnailUrl) {
        return MyPostResponse.builder()
                .postId(projection.getPostId())
                .boardType(projection.getBoardType())
                .category(projection.getCategory())
                .title(projection.getTitle())
                .contentPreview(toPreview(projection.getContent()))
                .thumbnailUrl(thumbnailUrl)
                .viewCount(projection.getViewCount())
                .commentCount(projection.getCommentCount())
                .createdAt(projection.getCreatedAt())
                .build();
    }

    // HTML 본문 → 태그·엔티티 제거한 순수 텍스트의 앞 PREVIEW_LENGTH자 (PostResponse.toPreview와 동일 정책)
    private static String toPreview(String html) {
        if (html == null) return "";
        String text = Jsoup.parse(html).text();
        return text.length() > PREVIEW_LENGTH ? text.substring(0, PREVIEW_LENGTH) : text;
    }
}