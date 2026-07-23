package com.paw.ddasoom.board.controller;

import com.paw.ddasoom.board.dto.request.CommentCreateRequest;
import com.paw.ddasoom.board.dto.request.CommentUpdateRequest;
import com.paw.ddasoom.board.dto.response.CommentResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.board.service.PostCommentService;
import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/comments")
@Tag(name = "게시판 댓글(PostComment)", description = "사용자 — 게시글 댓글 작성·목록·수정·삭제 API")
public class PostCommentController {

    private final PostCommentService postCommentService;

    /** 댓글 생성용 */
    @Operation(summary = "댓글 작성", description = """
            게시글에 댓글을 작성합니다. 작성 시 원글의 댓글 수(comment_count)가 함께 갱신됩니다.
            - 인가: USER(로그인 사용자 본인)""")
    @Parameter(name = "postId", description = "댓글을 달 게시글 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 유효성 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음 또는 삭제됨(BOARD_001) / 회원 없음(MEMBER_001)")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable(name = "postId") Long postId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CommentResponse response = postCommentService.create(postId, userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("댓글이 작성되었습니다.", response));
    }

    /** 댓글 조회용 - 20개 기준 */
    @Operation(summary = "댓글 목록 조회", description = """
            게시글의 활성 댓글을 페이징 조회합니다(기본 20건, 작성일 오름차순).
            - 인가: 공개(비로그인 가능)""")
    @Parameter(name = "postId", description = "게시글 PK", required = true, example = "1")
    @Parameter(name = "page", description = "페이지 번호(0부터 시작)", example = "0")
    @Parameter(name = "size", description = "페이지 크기", example = "20")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음 또는 삭제됨(BOARD_001)")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getComments(
            @PathVariable(name = "postId") Long postId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        PageResponse<CommentResponse> response = postCommentService.getComments(postId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 댓글 수정용 */
    @Operation(summary = "댓글 수정", description = """
            댓글 본문을 수정합니다. 작성자 본인만 수정할 수 있습니다.
            - 인가: USER(작성자 본인만)""")
    @Parameter(name = "postId", description = "원글 게시글 PK", required = true, example = "1")
    @Parameter(name = "commentId", description = "수정 대상 댓글 PK", required = true, example = "10")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 유효성 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "댓글 작성자 아님(BOARD_005)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음(BOARD_001) / 댓글 없음(BOARD_004)")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable(name = "postId") Long postId,
            @PathVariable(name = "commentId") Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CommentResponse response = postCommentService.update(postId, commentId, userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.success("댓글이 수정되었습니다.", response));
    }

    /** 댓글 삭제용 */
    @Operation(summary = "댓글 삭제", description = """
            댓글을 삭제(soft delete)합니다. 작성자 본인만 삭제할 수 있으며, 원글의 댓글 수가 함께 갱신됩니다.
            - 인가: USER(작성자 본인만)""")
    @Parameter(name = "postId", description = "원글 게시글 PK", required = true, example = "1")
    @Parameter(name = "commentId", description = "삭제 대상 댓글 PK", required = true, example = "10")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "댓글 작성자 아님(BOARD_005)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음(BOARD_001) / 댓글 없음(BOARD_004)")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable(name = "postId") Long postId,
            @PathVariable(name = "commentId") Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        postCommentService.delete(postId, commentId, userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다."));
    }
}