package com.paw.ddasoom.statistics.dto;

import com.paw.ddasoom.animal.domain.AnimalKind;

/** 종별 도넛 — D(개)/C(고양이) 2분류, 0건 종도 포함해 항상 2개 반환 (한글 라벨 변환은 프론트) */
public record AnimalKindRatioResponse(AnimalKind kind, long count) {}