package com.paw.ddasoom.image.controller;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.image.domain.OwnerType;
import com.paw.ddasoom.image.dto.response.ImageResponse;
import com.paw.ddasoom.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Tag(name = "이미지(Image)", description = "이미지 업로드 API — 소유자 확정 전 임시 저장 후 각 도메인 저장 시점에 연결")
@SecurityRequirement(name = "bearerAuth")   // 업로드는 로그인 필수 (업로더를 토큰의 memberId로 기록)
public class ImageController {

    private final ImageService imageService;

    /**
     * 이미지 업로드 - OwnerType 필요 (게시글, qna, 공지사항, 유기동물 정보 등)
     *
     * <p>업로더를 토큰의 memberId로 기록한다 — owner_id가 NULL인 동안 이 이미지를 확정 연결할 수 있는
     * 사람을 업로더 본인으로 한정하기 위함(임시 이미지 탈취 방지).
     * {@code /api/images}는 USER 전용 등록 경로라 {@code userDetails}가 null일 수 없다.
     */
    @Operation(summary = "이미지 업로드", description = """
            이미지 파일 1장을 업로드하고 imageId와 URL을 반환합니다.
            이 시점에는 소유자가 정해지지 않은 임시 상태(owner_id NULL)로 저장되며,
            게시글 등 소유자 저장 요청의 imageIds에 담아 보내면 확정 연결됩니다.
            업로더는 토큰의 memberId로 기록되어, 확정 연결은 업로더 본인만 할 수 있습니다.

            버킷/URL 분기는 ownerType이 결정합니다.
            - 공개(POST / NOTICE / ANIMAL / FAQ): 영구 정적 URL
            - 비공개(QNA / QNA_COMMENT): 30분 만료 Presigned URL

            제약: jpeg/png/gif/webp만 허용, 파일당 최대 10MB, 소유자당 최대 20장(연결 시점에 검증)
            - 인가: USER(로그인 사용자 본인)""")
    @Parameter(name = "file", description = "업로드할 이미지 파일 (jpeg/png/gif/webp, 최대 10MB)", required = true)
    @Parameter(name = "ownerType", description = "이미지가 연결될 소유자 타입", required = true,
            example = "POST")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "업로드 성공 (data = imageId, url, isThumbnail)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 형식(IMAGE_001) / 10MB 초과(IMAGE_002) / file·ownerType 누락 또는 ownerType 오타(INVALID_INPUT)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "MinIO 업로드 또는 URL 발급 실패(IMAGE_005)")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageResponse>> upload(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam("ownerType") OwnerType ownerType) {

        ImageResponse response = imageService.upload(file, ownerType, userDetails.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("이미지가 업로드되었습니다.", response));
    }
}