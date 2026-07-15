package com.paw.ddasoom.member.domain;

/**
 * 공지사항 이벤트 참여 상태.
 * board 도메인이 유저별 게시글 작성 수를 기준으로 status를 전이시킨다.
 * 확장 시 값만 추가하면 됨 (예: WINNER 당첨, EXCLUDED 제외 등).
 */
public enum EventStatus {
  NONE,          // 미참여 (기본값 — 이벤트 조건 미충족)
  PARTICIPATING  // 참여중 (게시글 작성 수 등 조건 충족)
}
