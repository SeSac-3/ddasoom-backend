package com.paw.ddasoom.board.controller;

import com.paw.ddasoom.board.dto.request.CommentCreateRequest;
import com.paw.ddasoom.board.dto.request.CommentUpdateRequest;
import com.paw.ddasoom.board.dto.response.CommentResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.board.service.PostCommentService;
import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/comments")
public class PostCommentController {

    private final PostCommentService postCommentService;

    /** 댓글 생성용 */
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CommentResponse response = postCommentService.create(postId, userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("댓글이 작성되었습니다.", response));
    }

    /** 댓글 조회용 - 20개 기준 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<CommentResponse> response = postCommentService.getComments(postId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 댓글 수정용 */
    @PatchMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CommentResponse response = postCommentService.update(postId, commentId, userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.success("댓글이 수정되었습니다.", response));
    }

    /** 댓글 삭제용 */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        postCommentService.delete(postId, commentId, userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다."));
    }
}