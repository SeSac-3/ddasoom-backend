package com.paw.ddasoom.board.exception;

import com.paw.ddasoom.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BoardErrorCode implements ErrorCode {

    // 게시글 관련 에러코드
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "BOARD_001", "해당 게시물은 없는 게시물입니다."),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "BOARD_002", "허용된 사용자만 접근 가능합니다."),

    // 게시글 타입 관련 에러코드
    INVALID_BOARD_TYPE(HttpStatus.BAD_REQUEST, "BOARD_003", "허용된 게시글 타입이 아닙니다."),

    // 댓글 관련 에러코드
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "BOARD_004", "해당 댓글은 없는 댓글입니다."),
    COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "BOARD_005", "해당 댓글은 허용된 사용자만 접근 가능합니다."),
    COMMENT_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "BOARD_006", "대댓글을 더 이상 작성할 수 없습니다."),

    // 대댓글 관련 에러코드
    PARENT_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "BOARD_007", "해당 대댓글은 없는 대댓글입니다."),

    // 카테고리 관련 에러코드
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "BOARD_008", "허용된 카테고리가 아닙니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
