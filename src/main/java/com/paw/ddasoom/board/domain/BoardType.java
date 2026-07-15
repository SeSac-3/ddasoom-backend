package com.paw.ddasoom.board.domain;


/** 보드 내 세부 카테고리 (예: 예방접종) — 종(강아지/고양이)은 boardType으로 분리됨. 값 목록 확정 전이라 String 유지 */
public enum BoardType {
    ADOPTION_REVIEW,   // 입양 후기
    DOG_INFO, // 강아지 정보
    CAT_INFO // 고양이 정보
}