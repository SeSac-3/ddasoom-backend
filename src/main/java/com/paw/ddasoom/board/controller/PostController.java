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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "게시판(Post)", description = "사용자 — 게시글 목록·상세·작성·수정·삭제 및 마이페이지(내 글/내 댓글) API")
public class PostController {

    private final PostService postService;
    private final PostCommentService postCommentService;

    /**
     * 마이페이지 — 내가 쓴 글 목록. boardType은 선택 필터(미전달 = 전체 보드).
     * ⚠️ /{postId}보다 정확 경로가 우선 매칭되므로 "/my"가 postId로 파싱될 일 없음 (fosters/my와 동일 패턴)
     */
    @Operation(summary = "내가 쓴 글 목록 조회", description = """
            마이페이지 — 로그인 사용자 본인이 작성한 게시글을 페이징 조회합니다.
            응답 항목에는 boardType과 대표 이미지 URL이 포함됩니다.
            - 인가: USER(로그인 사용자 본인)""")
    @Parameter(name = "boardType", description = "보드 타입 필터(선택). 미전달 시 전체 보드",
            example = "ADOPTION_REVIEW")
    @Parameter(name = "sort", description = "정렬 허용 필드: createdAt(기본, DESC) / viewCount / commentCount. "
            + "허용 외 값은 기본 정렬로 대체됩니다.", example = "createdAt,desc")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "허용되지 않은 boardType(BOARD_003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @SecurityRequirement(name = "bearerAuth")
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
    @Operation(summary = "내가 쓴 댓글 목록 조회", description = """
            마이페이지 — 로그인 사용자 본인이 작성한 댓글을 전체 게시글 대상으로 페이징 조회합니다.
            응답에는 원글 컨텍스트(postId/제목)가 함께 담깁니다.
            - 인가: USER(로그인 사용자 본인)""")
    @Parameter(name = "sort", description = "정렬 허용 필드: createdAt(기본, DESC)", example = "createdAt,desc")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @SecurityRequirement(name = "bearerAuth")
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
    @Operation(summary = "게시글 목록 조회", description = """
            보드 단위 게시글 목록을 페이징 조회합니다(기본 9건). 각 항목에 대표 이미지 URL이 포함됩니다.
            category/keyword는 선택 필터이며, keyword는 제목 부분일치 검색입니다.
            목록 필터의 category는 검증하지 않습니다 — 미존재 값이면 빈 결과가 반환됩니다.
            - 인가: 공개(비로그인 가능)""")
    @Parameter(name = "boardType", description = "보드 타입(필수)", required = true,
            example = "ADOPTION_REVIEW")
    @Parameter(name = "category", description = "카테고리 필터(선택). ADOPTION_REVIEW=강아지/고양이, "
            + "DOG_INFO·CAT_INFO=예방접종", example = "강아지")
    @Parameter(name = "keyword", description = "제목 부분일치 검색어(선택). 공백만 입력하면 검색 없음으로 처리",
            example = "입양")
    @Parameter(name = "sort", description = "정렬 허용 필드: createdAt(기본, DESC) / viewCount / commentCount",
            example = "createdAt,desc")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "허용되지 않은 boardType(BOARD_003)")
    })
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
    @Operation(summary = "게시글 상세 조회", description = """
            게시글 본문과 활성 이미지 목록을 조회합니다.
            조회수는 뷰어(memberId) 단위로 중복 제거되어 집계되며, 비로그인 조회는 집계되지 않습니다.
            - 인가: 공개(비로그인 가능)""")
    @Parameter(name = "postId", description = "게시글 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음 또는 삭제됨(BOARD_001)")
    })
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
    @Operation(summary = "게시글 작성", description = """
            게시글을 등록합니다. 본문 HTML은 서버에서 XSS 정제(sanitize) 후 저장됩니다.
            imageIds는 사전 업로드(POST /api/images)로 받은 값이며, 리스트 순서가 노출 순서로 저장됩니다.
            thumbnailImageId를 함께 보내면 대표 이미지로 지정됩니다.
            - 인가: USER(로그인 사용자 본인)""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공 (data = 생성된 게시글 PK)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 유효성 오류 / boardType 무효(BOARD_003) / 카테고리 무효(BOARD_008) / 이미지 20장 초과(IMAGE_003) / 첨부 불가 이미지 포함(IMAGE_006)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음(MEMBER_001) / 이미지 없음(IMAGE_004)")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PostCreateRequest request) {
        Long postId = postService.createPost(userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시글이 등록되었습니다.", postId));
    }

    /** 게시글 수정 */
    @Operation(summary = "게시글 수정", description = """
            게시글을 수정합니다. 작성자 본인만 수정할 수 있습니다.
            이미지는 diff 동기화 방식입니다 — 요청 imageIds에 없는 기존 이미지는 삭제(soft delete)되고, 순서가 갱신됩니다.
            thumbnailImageId는 전달했을 때만 재지정됩니다.
            - 인가: USER(작성자 본인만)""")
    @Parameter(name = "postId", description = "수정 대상 게시글 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 유효성 오류 / boardType 무효(BOARD_003) / 카테고리 무효(BOARD_008) / 이미지 20장 초과(IMAGE_003) / 첨부 불가 이미지 포함(IMAGE_006)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자 아님(BOARD_002)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음(BOARD_001) / 이미지 없음(IMAGE_004)")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> updatePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "postId") Long postId,
            @Valid @RequestBody PostUpdateRequest request) {
        postService.updatePost(userDetails.getMemberId(), postId, request);
        return ResponseEntity.ok(ApiResponse.success("게시글이 수정되었습니다."));
    }

    /** 게시글 삭제 (soft delete) - 삭제 시 조회 불가 */
    @Operation(summary = "게시글 삭제", description = """
            게시글을 삭제(soft delete)합니다. 작성자 본인만 삭제할 수 있으며, 삭제 후에는 조회되지 않습니다.
            연결된 이미지도 함께 정리됩니다.
            - 인가: USER(작성자 본인만)""")
    @Parameter(name = "postId", description = "삭제 대상 게시글 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자 아님(BOARD_002)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음(BOARD_001)")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "postId") Long postId) {
        postService.deletePost(userDetails.getMemberId(), postId);
        return ResponseEntity.ok(ApiResponse.success("게시글이 삭제되었습니다."));
    }
}