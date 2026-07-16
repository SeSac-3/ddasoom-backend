package com.paw.ddasoom.animal.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.animal.domain.AnimalGender;
import com.paw.ddasoom.animal.domain.AnimalKind;
import com.paw.ddasoom.animal.dto.request.AnimalListPageRequest;
import com.paw.ddasoom.animal.dto.request.AnimalNicknameUpdateRequest;
import com.paw.ddasoom.animal.dto.response.AnimalDetailPageResponse;
import com.paw.ddasoom.animal.dto.response.AnimalListPageResponse;
import com.paw.ddasoom.animal.dto.response.AnimalMainPageResponse;
import com.paw.ddasoom.animal.dto.response.AnimalMyPageResponse;
import com.paw.ddasoom.animal.service.AnimalDetailService;
import com.paw.ddasoom.animal.service.AnimalLikeService;
import com.paw.ddasoom.animal.service.AnimalListPageService;
import com.paw.ddasoom.animal.service.AnimalMainPageService;
import com.paw.ddasoom.animal.service.AnimalMyPageService;
import com.paw.ddasoom.animal.service.AnimalNicknameService;
import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
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
  private final AnimalLikeService animalLikeService; // 좋아요 서비스 (공부해야함)
  private final AnimalListPageService animalListPageService;
  private final AnimalDetailService animalDetailService; // 상세페이지 서비스 (공부해야함)
  private final AnimalMyPageService animalMyPageService; // 마이페이지 서비스 (공부해야함)
  private final AnimalMainPageService animalMainPageService; // 메인페이지 서비스 (공부해야함)

  /**
   * 닉네임 이름 수정 요청 시, 변경된 닉네임 저장 (임보 보호자용)
   */
  @PatchMapping("/{animalId}/nickname")
  public ResponseEntity<ApiResponse<Void>> updateNickname(
      @PathVariable Long animalId,
      @Valid @RequestBody AnimalNicknameUpdateRequest request) {
    animalNicknameService.updateNickname(animalId, request.nickname());
    log.info("닉네임 이름이 변경되었습니다: {}", request.nickname());
    return ResponseEntity.ok(ApiResponse.success("유기동물 이름이 변경되었습니다."));
  }

  /**
   * 좋아요 버튼 클릭 시 (Redis dirty 기록 — 배치가 RDB 반영)
   */
  @PostMapping("/{animalId}/likes")
  public ResponseEntity<ApiResponse<Void>> like(
    @PathVariable Long animalId,
    @AuthenticationPrincipal CustomUserDetails userDetails) {
    animalLikeService.like(animalId, userDetails.getMemberId());
    log.info("좋아요가 반영되었습니다. 동물ID: {}, 유저ID: {}", animalId, userDetails.getMemberId());
    return ResponseEntity.ok(ApiResponse.success("좋아요가 반영되었습니다."));
  }

  /**
   * 좋아요 취소 시
   */
  @DeleteMapping("/{animalId}/likes")
  public ResponseEntity<ApiResponse<Void>> unlike(
    @PathVariable Long animalId,
    @AuthenticationPrincipal CustomUserDetails userDetails) {
    animalLikeService.unlike(animalId, userDetails.getMemberId());
    log.info("좋아요가 취소되었습니다. 동물ID: {}, 유저ID: {}", animalId, userDetails.getMemberId());
    return ResponseEntity.ok(ApiResponse.success("좋아요가 취소되었습니다."));
  }

  /**
   * 유기동물 목록 조회 (동적 검색 + 페이징). 공개 API — 로그인 시 isLiked 계산.
   */
  @GetMapping("/list")
  public ResponseEntity<ApiResponse<PageResponse<AnimalListPageResponse>>> showAnimalList(
    @RequestParam(name = "kind", required = false) AnimalKind kind,
    @RequestParam(name = "location", required = false) String location,
    // primitive boolean은 미전달 시 항상 false로 바인딩돼 "필터 미적용"이 불가능 → Boolean으로 받아 null 허용
    @RequestParam(name = "isFostered", required = false) Boolean isFostered,
    @RequestParam(name = "isLiked", required = false) Boolean isLiked,
    @RequestParam(name = "gender", required = false) AnimalGender gender,
    @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails userDetails,
    @RequestParam(name = "page", defaultValue = "0") int page,
    @RequestParam(name = "size", defaultValue = "10") int size
  ) {
    Long memberId = userDetails != null ? userDetails.getMemberId() : null;
    AnimalListPageRequest request = AnimalListPageRequest.builder()
      .kind(kind)
      .location(location)
      .isFostered(isFostered)
      .isLiked(isLiked)
      .gender(gender)
      .build();
    return ResponseEntity.ok(ApiResponse.success(
        animalListPageService.search(request, memberId, PageRequest.of(page, size))));
  }

  /**
   * 메인페이지 미리보기 — 최근 등록 4건 (공개, 비로그인 노출)
   */
  @GetMapping("/main")
  public ResponseEntity<ApiResponse<List<AnimalMainPageResponse>>> getMainPreview() {
    return ResponseEntity.ok(ApiResponse.success(animalMainPageService.getMainPreview()));
  }

  /**
   * 유기동물 상세 조회. 공개 API — 로그인 시 isLiked 계산.
   */
  @GetMapping("/{animalId}")
  public ResponseEntity<ApiResponse<AnimalDetailPageResponse>> getAnimalDetail(
    @PathVariable Long animalId,
    @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails userDetails
  ) {
    Long memberId = userDetails != null ? userDetails.getMemberId() : null;
    return ResponseEntity.ok(ApiResponse.success(
        animalDetailService.getDetail(animalId, memberId)));
  }

  /**
   * 마이페이지 — 내가 좋아요한 동물 목록 (인증 필요)
   */
  @GetMapping("/me/likes")
  public ResponseEntity<ApiResponse<PageResponse<AnimalMyPageResponse>>> getMyLikedAnimals(
    @AuthenticationPrincipal CustomUserDetails userDetails,
    @RequestParam(name = "page", defaultValue = "0") int page,
    @RequestParam(name = "size", defaultValue = "10") int size
  ) {
    return ResponseEntity.ok(ApiResponse.success(
        animalMyPageService.getMyLikedAnimals(userDetails.getMemberId(), PageRequest.of(page, size))));
  }
}
