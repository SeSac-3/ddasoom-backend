package com.paw.ddasoom.dashboard.dto;

import com.paw.ddasoom.foster.domain.FosterStatus;

/** 임보 상태 분포 막대차트의 막대 하나 — 5종 전부 내려가며 0건도 포함 (차트 카테고리 고정) */
public record FosterStatusCountResponse(FosterStatus status, long count) {}