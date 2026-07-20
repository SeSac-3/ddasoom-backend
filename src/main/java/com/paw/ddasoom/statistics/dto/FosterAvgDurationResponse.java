package com.paw.ddasoom.statistics.dto;

/** 평균 임보 지속기간(일) — 소수 1자리. sampleCount 0이면 averageDays 0.0 (프론트 "데이터 없음" 표시 근거) */
public record FosterAvgDurationResponse(double averageDays, long sampleCount) {}