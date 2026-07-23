package com.paw.ddasoom.board.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.board.dto.response.AdminCommentResponse;
import com.paw.ddasoom.board.dto.response.AdminPostDetailResponse;
import com.paw.ddasoom.board.dto.response.AdminPostResponse;
import com.paw.ddasoom.board.service.AdminPostService;
import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.common.util.PageableSanitizer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

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
@Tag(name = "게시판 관리(Admin Post)", description = "관리자 — 게시글/댓글 조회 및 강제삭제 API")
@SecurityRequirement(name = "bearerAuth")   // /api/admin/** 는 SecurityConfig가 URL 레벨에서 ADMIN 잠금
public class AdminPostController {

    private final AdminPostService adminPostService;

    /**
     * 게시글 목록 — 전 보드 통합, 삭제 글 포함. boardType/keyword(제목 부분일치)는 선택 필터, 최신순.
     * (데모 규모라 프론트가 전체 1회 로드 후 클라이언트에서 검색/정렬/페이징 — AdminMemberList와 동일)
     */
    @Operation(summary = "게시글 목록 조회(관리자)", description = """
            전 보드 통합 게시글 목록을 조회합니다. 사용자용과 달리 삭제된 글도 포함됩니다.
            boardType/keyword(제목 부분일치)는 선택 필터입니다.
            - 인가: ADMIN""")
    @Parameter(name = "boardType", description = "보드 타입 필터(선택). 미전달 시 전체 보드",
            example = "ADOPTION_REVIEW")
    @Parameter(name = "keyword", description = "제목 부분일치 검색어(선택)", example = "입양")
    @Parameter(name = "sort", description = "정렬 허용 필드: createdAt(기본, DESC) / id / title / boardType "
            + "/ category / member.nickname / viewCount / commentCount / deletedAt "
            + "(deletedAt ASC = 활성 글 먼저)", example = "createdAt,desc")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "허용되지 않은 boardType(BOARD_003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminPostResponse>>> getPosts(
            @RequestParam(name = "boardType", required = false) String boardType,
            @RequestParam(name = "keyword", required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        // 정렬 화이트리스트 — 엔티티 경로로 지정한다.
        //  · member.nickname : 작성자 정렬(루트 Post 기준 중첩 경로 → 암시적 조인으로 해석됨)
        //  · deletedAt       : 화면의 "상태" 정렬. MySQL은 NULL을 가장 작게 보므로
        //                      ASC = 활성(null) 먼저 → 삭제됨 나중, 즉 기존 파생 정렬(0/1)과 순서가 같다
        Pageable safePageable = PageableSanitizer.sanitize(pageable,
                Sort.by(Sort.Direction.DESC, "createdAt"),
                "id", "title", "boardType", "category", "member.nickname",
                "viewCount", "commentCount", "createdAt", "deletedAt");
        return ResponseEntity.ok(ApiResponse.success(
                adminPostService.getPosts(boardType, keyword, safePageable)));
    }

    /** 게시글 상세 — 삭제 글도 조회 가능. 관리자 열람은 조회수를 올리지 않는다. */
    @Operation(summary = "게시글 상세 조회(관리자)", description = """
            게시글 상세를 조회합니다. 삭제된 글도 조회할 수 있으며, 관리자 열람은 조회수를 올리지 않습니다.
            - 인가: ADMIN""")
    @Parameter(name = "postId", description = "게시글 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음(BOARD_001)")
    })
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<AdminPostDetailResponse>> getPostDetail(
            @PathVariable(name = "postId") Long postId) {
        return ResponseEntity.ok(ApiResponse.success(adminPostService.getPostDetail(postId)));
    }

    /** 특정 게시글의 댓글 목록 — 삭제 댓글 포함, 오래된 순. */
    @Operation(summary = "특정 게시글의 댓글 목록 조회(관리자)", description = """
            한 게시글에 달린 댓글을 조회합니다. 삭제된 댓글도 포함되며 오래된 순으로 정렬됩니다.
            - 인가: ADMIN""")
    @Parameter(name = "postId", description = "게시글 PK", required = true, example = "1")
    @Parameter(name = "page", description = "페이지 번호(0부터 시작)", example = "0")
    @Parameter(name = "size", description = "페이지 크기", example = "20")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음(BOARD_001)")
    })
    @GetMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<AdminCommentResponse>>> getComments(
            @PathVariable(name = "postId") Long postId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminPostService.getComments(postId, PageableSanitizer.of(page, size))));
    }

    /** 게시글 강제삭제(soft delete) — 작성자 검증 없이 관리자 권한으로 수행. 이미 삭제 글이면 멱등 no-op. */
    @Operation(summary = "게시글 강제삭제(관리자)", description = """
            작성자 검증 없이 게시글을 삭제(soft delete)합니다. 이미 삭제된 글이면 멱등하게 아무 일도 하지 않습니다.
            - 인가: ADMIN""")
    @Parameter(name = "postId", description = "삭제 대상 게시글 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음(BOARD_001)")
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> forceDeletePost(
            @PathVariable(name = "postId") Long postId) {
        adminPostService.forceDeletePost(postId);
        return ResponseEntity.ok(ApiResponse.success("게시글이 강제삭제되었습니다."));
    }

    /** 댓글 강제삭제(soft delete) — 작성자 검증 없이 관리자 권한으로 수행. 이미 삭제 댓글이면 멱등 no-op. */
    @Operation(summary = "댓글 강제삭제(관리자)", description = """
            작성자 검증 없이 댓글을 삭제(soft delete)합니다. 이미 삭제된 댓글이면 멱등하게 아무 일도 하지 않습니다.
            관리자 전체 댓글 목록에서도 이 엔드포인트를 재사용합니다.
            - 인가: ADMIN""")
    @Parameter(name = "postId", description = "원글 게시글 PK", required = true, example = "1")
    @Parameter(name = "commentId", description = "삭제 대상 댓글 PK", required = true, example = "10")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음(BOARD_004)")
    })
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> forceDeleteComment(
            @PathVariable(name = "postId") Long postId,
            @PathVariable(name = "commentId") Long commentId) {
        adminPostService.forceDeleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success("댓글이 강제삭제되었습니다."));
    }
}