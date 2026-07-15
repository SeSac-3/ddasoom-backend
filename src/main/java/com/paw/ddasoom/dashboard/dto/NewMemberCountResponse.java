package com.paw.ddasoom.dashboard.dto;

// 대시보드 "금일 신규 가입자" 카드 — 클릭 시 회원 목록으로 이동(프론트 라우팅)
public record NewMemberCountResponse(long todayCount) {}
