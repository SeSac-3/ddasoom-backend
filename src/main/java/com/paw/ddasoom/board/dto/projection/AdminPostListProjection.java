package com.paw.ddasoom.board.dto.projection;

import com.paw.ddasoom.board.domain.BoardType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 게시글 목록 조회 전용 projection — API 응답 DTO 아님 (컨트롤러 밖으로 나가지 않음).
 *
 * <p>{@code PostRepository.findPostsForAdmin}의 JPQL SELECT NEW가 직접 생성하는 클래스로,
 * 목록에서 불필요한 무거운 컬럼(content 전체)을 DB 단계부터 제외하고 member를 JOIN으로 평면화
 * (닉네임만 추출)해 N+1을 방지하는 것이 존재 목적. (PostListProjection과 동일 전략)
 *
 * <p>사용자 목록용 {@link PostListProjection}과의 차이:
 * <ul>
 *   <li>{@code boardType} 포함 — 관리자 목록은 전 보드를 한 화면에 섞어 보여주므로 필요.</li>
 *   <li>{@code deletedAt} 포함 — 강제삭제/신고숨김된 글도 목록에 노출하고 상태를 뱃지로 표시하기 위함.
 *       (AdminMember 목록이 탈퇴 회원을 deletedAt으로 구분해 보여주는 것과 동일 철학)</li>
 *   <li>contentPreview 없음 — 관리자 목록은 제목 기준 모더레이션이라 본문 미리보기가 불필요.</li>
 * </ul>
 *
 * <p>from() 팩토리 없음: JPQL 생성자 표현식은 public 생성자 직접 호출만 가능
 * (컨벤션의 from() 규칙은 API 경계 DTO 대상이므로 미적용 — PostListProjection과 동일).
 */
@Getter
@AllArgsConstructor
public class AdminPostListProjection {

    private final Long postId;
    private final BoardType boardType;
    private final String category;
    private final String title;
    private final int viewCount;
    private final int commentCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime deletedAt;   // NULL = 활성, 값 있음 = 삭제됨(강제삭제/신고숨김/작성자삭제)
    private final Long authorId;
    private final String authorNickname;
}