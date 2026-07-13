package com.paw.ddasoom.image.exception;


import com.paw.ddasoom.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ImageErrorCode implements ErrorCode {

    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "IMAGE_001", "지원하지 않은 이미지 형식입니다. (jpeg, png, gif, webp만 가능)"),
    IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "IMAGE_002", "이미지 크기는 10MB를 초과할 수 없습니다."),
    IMAGE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "IMAGE_003", "이미지는 최대 5장까지 첨부 할 수 있습니다."),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMAGE_004", "이미지를 찾을 수 없습니다."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_005", "이미지 업로드에 실패했습니다."),
    IMAGE_OWNER_MISMATCH(HttpStatus.BAD_REQUEST, "IMAGE_006", "해당 게시물에 첨부할 수 없는 이미지가 포함되어 있습니다.");

    // 대표 이미지 지정(setThumbnail) 실패도 위 코드 재사용 — 신규 코드 불필요 (IMAGE_FLOW 3-3)
    // (미존재/삭제된 imageId → IMAGE_004, 소유자 불일치 → IMAGE_006)

    private final HttpStatus status;
    private final String code;
    private final String message;
}
