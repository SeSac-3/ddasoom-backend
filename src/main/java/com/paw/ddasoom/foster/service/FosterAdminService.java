package com.paw.ddasoom.foster.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.foster.domain.Foster;
import com.paw.ddasoom.foster.domain.FosterStatus;
import com.paw.ddasoom.foster.dto.request.FosterAdminUpdateRequest;
import com.paw.ddasoom.foster.dto.response.FosterAdminDetailResponse;
import com.paw.ddasoom.foster.dto.response.FosterAdminListResponse;
import com.paw.ddasoom.foster.exception.FosterErrorCode;
import com.paw.ddasoom.foster.exception.FosterException;
import com.paw.ddasoom.foster.repository.FosterRepository;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.service.MemberService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FosterAdminService {

  private final FosterRepository fosterRepository;
  private final MemberService memberService;

  /** 임시보호가 진행 중인 상태를 판단하는 목록 */
  private static final List<FosterStatus> ACTIVE_FOSTER_STATUSES = List.of(
    FosterStatus.FOSTERING, // 임시보호중
    FosterStatus.EXTENDED   // 임시보호연장
  );

  /** 관리자 임시보호신청 조회(디테일) */
  @Transactional(readOnly = true)
  public FosterAdminDetailResponse getFosterDetail(Long fosterId){
    Foster foster = fosterRepository.findById(fosterId)
                    .orElseThrow(() -> new FosterException(FosterErrorCode.FOSTER_NOT_FOUND));

    return FosterAdminDetailResponse.from(foster);
  }
  /** 관리자 임시보호신청 조회(리스트) */
  @Transactional(readOnly = true)
  public PageResponse<FosterAdminListResponse> getFosterList(
    FosterStatus status,
    boolean activeOnly,
    boolean includeDeleted,
    LocalDate startDate,
    LocalDate endDate,
    Pageable pageable){
    // 선택 조회와 함께 동시 조회시 충돌 방지 검증
    if(status != null && activeOnly){
      throw new FosterException(FosterErrorCode.INVALID_FOSTER_SEARCH_CONDITION);
    }
    // 기간 조건 오류 방지 검증(시작일이 종료일보다 늦을 경우)
    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
      throw new FosterException(FosterErrorCode.INVALID_FOSTER_DATE_RANGE);
    }
    
    
    LocalDateTime startAt = startDate != null ? startDate.atStartOfDay() : null;
    LocalDateTime endAt = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

    Page<Foster> fosterList = fosterRepository.findAllForAdmin(
      status,
      activeOnly,
      includeDeleted,
      startAt,
      endAt,
      pageable);

    return PageResponse.of(fosterList, FosterAdminListResponse::from);
  }

  /** 관리자 임시보호신청 수정 */
  @Transactional
  public void updateFoster(Long memberId, Long fosterId, FosterAdminUpdateRequest request){
    Foster foster = fosterRepository.findById(fosterId)
                    .orElseThrow(() -> new FosterException(FosterErrorCode.FOSTER_NOT_FOUND));
    Member reviewer = memberService.getMember(memberId);
    
    // 임시보호날짜 검증
    validateFosterPeriod(request);
    
    foster.updateAdminReview(
      reviewer,
      request.getAnswer(),
      request.getStatus(),
      request.getFosterStartAt(),
      request.getFosterEndAt(),
      request.getFosterExtendAt(),
      request.getFosterCompleteAt());
  
    syncAnimalFosterStatus(foster);
  }
  
  /** 관리자 수정 후 상태값에 따른 animal 데이터 임시보호 여부 변경 */
  private void syncAnimalFosterStatus(Foster foster){
    // 현재 animalId를 가져와 임시보호중인지 연장중인지 확인
    boolean isFostered = fosterRepository.existsByAnimal_IdAndStatusInAndDeletedAtIsNull(
      foster.getAnimal().getId(), ACTIVE_FOSTER_STATUSES);

    //animal에 임시보호 값 반영
    foster.getAnimal().updateFosteredStatus(isFostered);
  }

  /** 업데이트시 임시보호 기간 설정에 대한 검증 메서드 (논리적으로 맞지 않는 임시보호 날짜) */
  private void validateFosterPeriod(FosterAdminUpdateRequest request) {
  if (request.getFosterStartAt() != null
      && request.getFosterEndAt() != null
      && request.getFosterStartAt().isAfter(request.getFosterEndAt())) {
    throw new FosterException(FosterErrorCode.INVALID_FOSTER_PERIOD);
  }

  if (request.getFosterEndAt() != null
      && request.getFosterExtendAt() != null
      && request.getFosterEndAt().isAfter(request.getFosterExtendAt())) {
    throw new FosterException(FosterErrorCode.INVALID_FOSTER_PERIOD);
  }

  if (request.getFosterStartAt() != null
      && request.getFosterCompleteAt() != null
      && request.getFosterStartAt().isAfter(request.getFosterCompleteAt())) {
    throw new FosterException(FosterErrorCode.INVALID_FOSTER_PERIOD);
  }
}
}
