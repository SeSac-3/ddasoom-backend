package com.paw.ddasoom.board.controller;

import com.paw.ddasoom.board.dto.request.PostCreateRequest;
import com.paw.ddasoom.board.dto.request.PostUpdateRequest;
import com.paw.ddasoom.board.dto.response.PostDetailResponse;
import com.paw.ddasoom.board.dto.response.PostResponse;
import com.paw.ddasoom.board.service.PostService;
import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /** 전체 페이지 조회(기본 페이지네이션: 9), 카테고리, 보드타입 필요*/
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getPostList(
            @RequestParam String boardType,
            @RequestParam String category,
            @PageableDefault(size = 9) Pageable pageable) {
        PageResponse<PostResponse> response = postService.getPostList(boardType, category, pageable);
        return ResponseEntity.ok(ApiResponse.success("게시글 목록을 조회했습니다.", response));
    }

    /** 게시글 상세 조회 */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(@PathVariable Long postId) {
        PostDetailResponse response = postService.getPostDetail(postId);
        return ResponseEntity.ok(ApiResponse.success("게시글을 조회했습니다.", response));
    }

    /** 게시글 생성 */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PostCreateRequest request) {
        Long postId = postService.createPost(userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시글이 등록되었습니다.", postId));
    }

    /** 게시글 수정 */
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> updatePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @RequestBody PostUpdateRequest request) {
        postService.updatePost(userDetails.getMemberId(), postId, request);
        return ResponseEntity.ok(ApiResponse.success("게시글이 수정되었습니다."));
    }

    /** 게시글 삭제 (soft delete) - 삭제 시 조회 불가 */
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId) {
        postService.deletePost(userDetails.getMemberId(), postId);
        return ResponseEntity.ok(ApiResponse.success("게시글이 삭제되었습니다."));
    }
}