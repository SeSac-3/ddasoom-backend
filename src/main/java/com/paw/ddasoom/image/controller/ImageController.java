package com.paw.ddasoom.image.controller;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.image.domain.OwnerType;
import com.paw.ddasoom.image.dto.response.ImageResponse;
import com.paw.ddasoom.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /** 이미지 업로드 - OwnerType 필요 (게시글, qna, 공지사항, 유기동물 정보 등)*/
    @PostMapping
    public ResponseEntity<ApiResponse<ImageResponse>> upload(
            @RequestParam MultipartFile file,
            @RequestParam OwnerType ownerType) {

        ImageResponse response = imageService.upload(file, ownerType);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("이미지가 업로드되었습니다.", response));
    }
}