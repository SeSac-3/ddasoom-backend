package com.paw.ddasoom.animal.service;

/**
 * service 패키지 - dto/response 아님 (외부 경계가 아니라 내부 배치용)
 * Redis dirty 해시 -> 파싱한 배치 대상
*/
public record AnimalLikeSyncItem(
  Long animalId,
  Long memberId
) {}
