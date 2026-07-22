package com.paw.ddasoom.board.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.board.dto.request.PostCreateRequest;
import com.paw.ddasoom.board.dto.request.PostUpdateRequest;
import com.paw.ddasoom.board.dto.response.MyCommentResponse;
import com.paw.ddasoom.board.dto.response.MyPostResponse;
import com.paw.ddasoom.board.dto.response.PostDetailResponse;
import com.paw.ddasoom.board.dto.response.PostResponse;
import com.paw.ddasoom.board.service.PostCommentService;
import com.paw.ddasoom.board.service.PostService;
import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.common.util.PageableSanitizer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostCommentService postCommentService;

    /**
     * 마이페이지 — 내가 쓴 글 목록. boardType은 선택 필터(미전달 = 전체 보드).
     * ⚠️ /{postId}보다 정확 경로가 우선 매칭되므로 "/my"가 postId로 파싱될 일 없음 (fosters/my와 동일 패턴)
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<MyPostResponse>>> getMyPosts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "boardType", required = false) String boardType,
            @PageableDefault(size = 10) Pageable pageable) {
        Pageable safePageable = PageableSanitizer.sanitize(pageable,
                Sort.by(Sort.Direction.DESC, "createdAt"), "createdAt", "viewCount", "commentCount");
        PageResponse<MyPostResponse> response =
                postService.getMyPosts(userDetails.getMemberId(), boardType, safePageable);
        return ResponseEntity.ok(ApiResponse.success("내가 쓴 글 목록을 조회했습니다.", response));
    }

    /**
     * 마이페이지 — 내가 쓴 댓글 목록 (전체 게시글 대상).
     * PostCommentController는 /api/posts/{postId}/comments 하위라 postId 없는 이 경로를 가질 수 없어
     * PostController에 위치 (URL 소속: /api/posts 하위 유지).
     */
    @GetMapping("/comments/my")
    public ResponseEntity<ApiResponse<PageResponse<MyCommentResponse>>> getMyComments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        // 댓글은 정렬 기준이 작성 시각뿐 — 화이트리스트도 createdAt 하나
        Pageable safePageable = PageableSanitizer.sanitize(pageable,
                Sort.by(Sort.Direction.DESC, "createdAt"), "createdAt");
        PageResponse<MyCommentResponse> response =
                postCommentService.getMyComments(userDetails.getMemberId(), safePageable);
        return ResponseEntity.ok(ApiResponse.success("내가 쓴 댓글 목록을 조회했습니다.", response));
    }

    /** 전체 페이지 조회(기본 페이지네이션: 9), 카테고리, 보드타입 필요. keyword는 제목 부분일치 검색(선택) */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getPostList(
            @RequestParam(name = "boardType") String boardType,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "keyword", required = false) String keyword,
            @PageableDefault(size = 9) Pageable pageable) {
        Pageable safePageable = PageableSanitizer.sanitize(pageable,
                Sort.by(Sort.Direction.DESC, "createdAt"), "createdAt", "viewCount", "commentCount");
        PageResponse<PostResponse> response =
                postService.getPostList(boardType, category, keyword, safePageable);
        return ResponseEntity.ok(ApiResponse.success("게시글 목록을 조회했습니다.", response));
    }


    /**
     * 게시글 상세 조회 — 비로그인도 열람 가능.
     * 조회수는 뷰어(memberId) 단위로 중복 제거되어 집계되며, 비로그인 조회는 집계되지 않는다 (PostService 참고).
     */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(
            @PathVariable(name = "postId") Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 공개 경로라 비로그인 요청에서는 userDetails가 null로 들어온다.
        Long viewerId = userDetails != null ? userDetails.getMemberId() : null;
        PostDetailResponse response =
                postService.getPostDetail(postId, viewerId);
        return ResponseEntity.ok(ApiResponse.success("게시글을 조회했습니다.", response));
    }

    /** 게시글 생성 */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PostCreateRequest request) {
        Long postId = postService.createPost(userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시글이 등록되었습니다.", postId));
    }

    /** 게시글 수정 */
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> updatePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "postId") Long postId,
            @Valid @RequestBody PostUpdateRequest request) {
        postService.updatePost(userDetails.getMemberId(), postId, request);
        return ResponseEntity.ok(ApiResponse.success("게시글이 수정되었습니다."));
    }

    /** 게시글 삭제 (soft delete) - 삭제 시 조회 불가 */
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "postId") Long postId) {
        postService.deletePost(userDetails.getMemberId(), postId);
        return ResponseEntity.ok(ApiResponse.success("게시글이 삭제되었습니다."));
    }
}