package com.paw.ddasoom.board.dto.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;


/**
 * 게시글 목록 조회 전용 projection — API 응답 DTO 아님 (컨트롤러 밖으로 나가지 않음).
 *
 * PostRepository.findPostList의 JPQL SELECT NEW가 직접 생성하는 클래스로,
 * 목록에서 불필요한 무거운 컬럼(content 전체)을 DB 단계부터 제외하고
 * member를 JOIN으로 평면화(닉네임만 추출)해 N+1을 방지하는 것이 존재 목적.
 *
 * - contentPreview: content의 앞 200자만 SUBSTRING (카드 2줄 미리보기용, 최종 말줄임은 프론트 CSS 책임)
 * - 썸네일 URL 없음: image는 별도 도메인이라 Service에서 배치 조회 후 PostResponse로 조합
 * - from() 팩토리 없음: JPQL 생성자 표현식은 public 생성자 직접 호출만 가능 (컨벤션의 from() 규칙은
 *   API 경계 DTO 대상이므로 미적용)
 */
@Getter
@AllArgsConstructor
public class PostListProjection {

    private final Long postId;
    private final String category;
    private final String title;
    private final String content;
    private final int viewCount;
    private final int commentCount;
    private final LocalDateTime createdAt;
    private final Long authorId;
    private final String authorNickname;
}
