package com.paw.ddasoom.animal.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.animal.dto.request.AnimalNicknameUpdateRequest;
import com.paw.ddasoom.animal.dto.response.AnimalSyncResponse;
import com.paw.ddasoom.animal.service.AnimalLikeService;
import com.paw.ddasoom.animal.service.AnimalNicknameService;
import com.paw.ddasoom.animal.service.AnimalSyncService;
import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;

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
  private final AnimalLikeService animalLikeService;

  /**
   * API에서 받아온 모든 동물 정보를 DB에 저장
   * @return Httpstatus, message, DTO
   */
  @PostMapping("/sync")
  public ResponseEntity<ApiResponse<List<AnimalSyncResponse>>> syncAnimals() {
      List<Animal> savedAnimals = animalSyncService.syncAnimals();
      List<AnimalSyncResponse> response = savedAnimals.stream()
              .map(AnimalSyncResponse::from)
              .toList();

      log.info("API 동물 {}건이 DB에 저장/갱신되었습니다.", savedAnimals.size());
      return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 닉네임 이름 수정 요청 시, 변경된 닉네임 저장
   * @param animalId
   * @param request
   * @return Httpstatus, message, DTO
   */
  @PatchMapping("/{animalId}/nickname")
  public ResponseEntity<ApiResponse<Void>> updateNickname(
      @PathVariable Long animalId,
      @Valid @RequestBody AnimalNicknameUpdateRequest request) {
    animalNicknameService.updateNickname(animalId, request.nickname());
    return ResponseEntity.ok(ApiResponse.success("유기동물 이름이 변경되었습니다."));
  }

  /**
   * 좋아요 버튼 클릭 시, 좋아요 카운트 업데이트
   * @param animalId
   * @param userDetails
   * @param request
   * @return
   */
  @PostMapping("/{animalId}/likes")
  public ResponseEntity<ApiResponse<Void>> like(
    @PathVariable Long animalId,
    @AuthenticationPrincipal CustomUserDetails userDetails) {
    animalLikeService.like(animalId, userDetails.getMemberId());
    return ResponseEntity.ok(ApiResponse.success("좋아요가 반영되었습니다."));
  }

  /**
   * 좋아요 취소 시, 좋아요 카운트 업데이트
   * @param animalId
   * @param userDetails
   * @return
   */
  @DeleteMapping("/{animalId}/likes")
  public ResponseEntity<ApiResponse<Void>> unlike(
    @PathVariable Long animalId,
    @AuthenticationPrincipal CustomUserDetails userDetails) {
    animalLikeService.unlike(animalId, userDetails.getMemberId());
    return ResponseEntity.ok(ApiResponse.success("좋아요가 취소되었습니다."));
  }
  
}
