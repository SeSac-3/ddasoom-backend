package com.paw.ddasoom.animal.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.animal.service.AnimalSyncService;
import com.paw.ddasoom.common.dto.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 관리자용 유기동물 API.
 * /api/admin 하위 = SecurityConfig가 ADMIN으로 자동 잠금 (SECURITY-FLOW 규칙 5)
 * 공공데이터포털 수동 미러링 동기화 버튼이 이 엔드포인트를 호출한다.
 */
@RestController
@RequestMapping("/api/admin/animals")
@RequiredArgsConstructor
@Slf4j
public class AdminAnimalController {

  private final AnimalSyncService animalSyncService;

  @PostMapping("/sync")
  public ResponseEntity<ApiResponse<Void>> syncAnimals() {
    List<Animal> savedAnimals = animalSyncService.syncAnimals();
    log.info("API 동물 {}건이 DB에 저장/갱신되었습니다.", savedAnimals.size());
    return ResponseEntity.ok(ApiResponse.success("API 유기동물이 DB에 저장/갱신되었습니다."));
  }
}
