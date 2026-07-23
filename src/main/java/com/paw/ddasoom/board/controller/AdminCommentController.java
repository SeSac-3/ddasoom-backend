package com.paw.ddasoom.board.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.board.domain.BoardType;
import com.paw.ddasoom.board.dto.response.AdminAllCommentResponse;
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
@Tag(name = "전체 댓글 관리(Admin Comment)", description = "관리자 — 사이트 전체 댓글 통합 목록 API")
@SecurityRequirement(name = "bearerAuth")   // /api/admin/** 는 SecurityConfig가 URL 레벨에서 ADMIN 잠금
public class AdminCommentController {

    private final AdminPostService adminPostService;

    /**
     * 전체 댓글 목록 — 전 게시글 통합, 삭제 댓글 포함, 최신순. 원글 컨텍스트(postId/제목/보드타입) 포함.
     * (데모 규모라 프론트가 전체 1회 로드 후 클라이언트에서 검색/필터/정렬/페이징 — AdminPostList와 동일)
     */
    @Operation(summary = "전체 댓글 목록 조회(관리자)", description = """
            전 게시글의 댓글을 통합 조회합니다. 삭제된 댓글도 포함되며 최신순입니다.
            각 항목에 원글 컨텍스트(postId/제목/보드타입)가 포함됩니다.
            강제삭제는 DELETE /api/admin/posts/{postId}/comments/{commentId} 를 재사용합니다.
            - 인가: ADMIN""")
    @Parameter(name = "boardType", description = "보드 타입 필터(선택). 미전달 시 전체 보드",
            example = "ADOPTION_REVIEW")
    @Parameter(name = "keyword", description = "검색어(선택)", example = "감사")
    @Parameter(name = "sort", description = "정렬 허용 필드: createdAt(기본, DESC) / id / content "
            + "/ member.nickname / post.title / post.boardType / deletedAt", example = "createdAt,desc")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "허용되지 않은 boardType 값(INVALID_INPUT — enum 직접 바인딩이라 BOARD_003이 아님)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminAllCommentResponse>>> getComments(
            @RequestParam(name = "boardType", required = false) BoardType boardType,
            @RequestParam(name = "keyword", required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        // 이 화면은 정렬 UI가 없어 화이트리스트를 비워 기본 정렬(작성일 최신순)로 고정한다.
        // 나중에 정렬을 추가하면 허용 프로퍼티만 여기에 나열하면 된다.
        // 정렬 화이트리스트 — 루트는 PostComment(c). 중첩 경로는 암시적 조인으로 해석된다.
        Pageable safePageable = PageableSanitizer.sanitize(pageable,
                Sort.by(Sort.Direction.DESC, "createdAt"),
                "id", "content", "member.nickname", "post.title", "post.boardType",
                "createdAt", "deletedAt");
        return ResponseEntity.ok(ApiResponse.success(
                adminPostService.getAllComments(boardType, keyword, safePageable)));
    }


}