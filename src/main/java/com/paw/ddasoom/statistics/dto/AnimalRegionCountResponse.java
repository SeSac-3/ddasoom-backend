package com.paw.ddasoom.statistics.dto;

/** 보호지역 시/도별 분포 — 건수 내림차순. 임보 신청자 주소 분포는 데이터 축적 후 확장 (통계요청 2-5) */
public record AnimalRegionCountResponse(String region, long count) {}