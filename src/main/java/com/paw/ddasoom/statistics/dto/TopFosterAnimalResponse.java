package com.paw.ddasoom.statistics.dto;

import com.paw.ddasoom.animal.domain.AnimalKind;

/** 임보 신청 TOP10 행 — 순위는 프론트가 배열 index로 표시. 종 포함(인사이트용 — 통계요청 2-6) */
public record TopFosterAnimalResponse(
        Long animalId, String nickname, String abandonmentId,
        AnimalKind kind, String typeName, String imageUrl, long fosterCount) {}