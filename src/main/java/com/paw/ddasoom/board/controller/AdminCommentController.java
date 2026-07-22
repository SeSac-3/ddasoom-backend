package com.paw.ddasoom.board.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.board.dto.response.AdminAllCommentResponse;
import com.paw.ddasoom.board.service.AdminPostService;
import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.common.util.PageableSanitizer;

import lombok.RequiredArgsConstructor;

/**
 * [관리자용 전체 댓글 관리 API]
 * 사이트 전체 댓글을 게시글에 종속되지 않고 한 목록에서 조회한다.
 *
 * <p>{@link AdminPostController}(경로 {@code /api/admin/posts})와 URL 프리픽스가 달라
 * 컨트롤러를 분리했다. 다만 댓글 모더레이션 로직은 이미 {@link AdminPostService}가 소유하고 있어
 * (게시글 상세 안 댓글 조회·강제삭제가 거기 있음), 읽기 한 건을 위해 별도 서비스를 만들지 않고 위임한다.
 *
 * <p><b>강제삭제는 이 컨트롤러에 없다</b> — 기존 {@code DELETE /api/admin/posts/{postId}/comments/{commentId}}
 * 를 그대로 재사용한다(목록 행이 postId를 함께 내려주므로 프론트가 그 경로를 호출). 중복 엔드포인트를 만들지 않는다.
 *
 * <p>{@code /api/admin/**}는 SecurityConfig가 URL 레벨에서 ADMIN 잠금하므로 컨트롤러에 권한 체크 코드가 없다.
 */
@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final AdminPostService adminPostService;

    /**
     * 전체 댓글 목록 — 전 게시글 통합, 삭제 댓글 포함, 최신순. 원글 컨텍스트(postId/제목/보드타입) 포함.
     * (데모 규모라 프론트가 전체 1회 로드 후 클라이언트에서 검색/필터/정렬/페이징 — AdminPostList와 동일)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminAllCommentResponse>>> getComments(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminPostService.getAllComments(PageableSanitizer.of(page, size))));
    }

    
}