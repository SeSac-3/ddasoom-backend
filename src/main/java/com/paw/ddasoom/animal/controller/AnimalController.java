package com.paw.ddasoom.animal.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.animal.dto.request.AnimalNicknameUpdateRequest;
import com.paw.ddasoom.animal.dto.response.AnimalResponse;
import com.paw.ddasoom.animal.service.AnimalNicknameService;
import com.paw.ddasoom.animal.service.AnimalSyncService;
import com.paw.ddasoom.common.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/animals")
@RequiredArgsConstructor
@Slf4j
public class AnimalController {

  private final AnimalNicknameService animalNicknameService;
  private final AnimalSyncService animalSyncService;

  /**
   * API에서 받아온 모든 동물 정보를 DB에 저장
   * @return Httpstatus, message
   */
  @PostMapping("/sync")
  public ResponseEntity<ApiResponse<List<AnimalResponse>>> syncAnimals() {
      List<Animal> savedAnimals = animalSyncService.syncAnimals();
      List<AnimalResponse> response = savedAnimals.stream()
              .map(AnimalResponse::from)
              .toList();

      log.info("API 동물 {}건이 DB에 저장/갱신되었습니다.", savedAnimals.size());
      return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 닉네임 이름 수정 요청 시, 변경된 닉네임 저장
   * @param animalId
   * @param request
   * @return Httpstatus, message
   */
  @PatchMapping("/{animalId}/nickname")
  public ResponseEntity<ApiResponse<Void>> updateNickname(
      @PathVariable Long animalId,
      @Valid @RequestBody AnimalNicknameUpdateRequest request) {
    animalNicknameService.updateNickname(animalId, request.nickname());
    return ResponseEntity.ok(ApiResponse.success());
  }
  
}
