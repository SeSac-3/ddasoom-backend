package com.paw.ddasoom.image.controller;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.image.domain.OwnerType;
import com.paw.ddasoom.image.dto.response.ImageResponse;
import com.paw.ddasoom.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
public class ImageController {

    private final ImageService imageService;

    /**
     * 이미지 업로드 - OwnerType 필요 (게시글, qna, 공지사항, 유기동물 정보 등)
     *
     * <p>업로더를 토큰의 memberId로 기록한다 — owner_id가 NULL인 동안 이 이미지를 확정 연결할 수 있는
     * 사람을 업로더 본인으로 한정하기 위함(임시 이미지 탈취 방지).
     * {@code /api/images}는 USER 전용 등록 경로라 {@code userDetails}가 null일 수 없다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ImageResponse>> upload(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam("ownerType") OwnerType ownerType) {

        ImageResponse response = imageService.upload(file, ownerType, userDetails.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("이미지가 업로드되었습니다.", response));
    }
}