package com.paw.ddasoom.member.domain;

/**
 * 회원 노출 상태 — 신고 접수 시 관리자가 수동으로 전환한다 (자동 전이 없음).
 * ACTIVE: 정상 (기본값) / HIDDEN: 신고 제재로 숨김 처리
 * ※ 확장 예정 상태(보류중 등)는 리팩토링 요청 시점에 추가 — 현재는 2종 최소 스코프 (팀 결정)
 */
public enum MemberStatus {
    ACTIVE,
    HIDDEN
}