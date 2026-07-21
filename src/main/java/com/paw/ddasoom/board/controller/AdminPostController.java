package com.paw.ddasoom.board.controller;

import com.paw.ddasoom.board.dto.response.AdminCommentResponse;
import com.paw.ddasoom.board.dto.response.AdminPostDetailResponse;
import com.paw.ddasoom.board.dto.response.AdminPostResponse;
import com.paw.ddasoom.board.service.AdminPostService;
import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [관리자용 게시판 API]
 * 게시글/댓글 조회와 강제삭제를 담당한다. 사용자용 {@link PostController}/{@link PostCommentController}와
 * 컨트롤러만 분리(조회 대상·권한 모델이 달라 서비스도 분리 — {@link AdminPostService}).
 *
 * /api/admin/** 는 SecurityConfig가 URL 레벨에서 자동 ADMIN 잠금하므로 컨트롤러에 권한 체크 코드가 없다.
 * (AdminMemberController / AdminReportController와 동일 패턴)
 */
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final AdminPostService adminPostService;

    /**
     * 게시글 목록 — 전 보드 통합, 삭제 글 포함. boardType/keyword(제목 부분일치)는 선택 필터, 최신순.
     * (데모 규모라 프론트가 전체 1회 로드 후 클라이언트에서 검색/정렬/페이징 — AdminMemberList와 동일)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminPostResponse>>> getPosts(
            @RequestParam(name = "boardType", required = false) String boardType,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminPostService.getPosts(boardType, keyword, PageRequest.of(page, size))));
    }

    /** 게시글 상세 — 삭제 글도 조회 가능. 관리자 열람은 조회수를 올리지 않는다. */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<AdminPostDetailResponse>> getPostDetail(
            @PathVariable(name = "postId") Long postId) {
        return ResponseEntity.ok(ApiResponse.success(adminPostService.getPostDetail(postId)));
    }

    /** 특정 게시글의 댓글 목록 — 삭제 댓글 포함, 오래된 순. */
    @GetMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<AdminCommentResponse>>> getComments(
            @PathVariable(name = "postId") Long postId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminPostService.getComments(postId, PageRequest.of(page, size))));
    }

    /** 게시글 강제삭제(soft delete) — 작성자 검증 없이 관리자 권한으로 수행. 이미 삭제 글이면 멱등 no-op. */
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> forceDeletePost(
            @PathVariable(name = "postId") Long postId) {
        adminPostService.forceDeletePost(postId);
        return ResponseEntity.ok(ApiResponse.success("게시글이 강제삭제되었습니다."));
    }

    /** 댓글 강제삭제(soft delete) — 작성자 검증 없이 관리자 권한으로 수행. 이미 삭제 댓글이면 멱등 no-op. */
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> forceDeleteComment(
            @PathVariable(name = "postId") Long postId,
            @PathVariable(name = "commentId") Long commentId) {
        adminPostService.forceDeleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success("댓글이 강제삭제되었습니다."));
    }
}