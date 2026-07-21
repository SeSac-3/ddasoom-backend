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
import com.paw.ddasoom.foster.domain.FosterManagementScope;
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

  private static final List<FosterStatus> APPLICATION_STATUSES = List.of(
    FosterStatus.PENDING,
    FosterStatus.REJECTED
);

  private static final List<FosterStatus> PROGRESS_FOSTER_STATUSES = List.of(
      FosterStatus.FOSTERING,
      FosterStatus.EXTENDED,
      FosterStatus.ENDED
  );

  private static final List<FosterStatus> ACTIVE_FOSTER_STATUSES = List.of(
      FosterStatus.FOSTERING,
      FosterStatus.EXTENDED
  );

  private final FosterRepository fosterRepository;
  private final MemberService memberService;

  /** 관리자 임시보호 신청 상세 조회 */
  @Transactional(readOnly = true)
  public FosterAdminDetailResponse getFosterDetail(Long fosterId) {
    Foster foster = fosterRepository.findById(fosterId)
        .orElseThrow(() -> new FosterException(FosterErrorCode.FOSTER_NOT_FOUND));

    return FosterAdminDetailResponse.from(foster);
  }

    /** 관리자 임시보호 신청 목록 조회 */
  @Transactional(readOnly = true)
  public PageResponse<FosterAdminListResponse> getFosterList(
      FosterManagementScope scope,
      FosterStatus status,
      boolean activeOnly,
      boolean includeDeleted,
      LocalDate startDate,
      LocalDate endDate,
      Pageable pageable
  ) {
    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
      throw new FosterException(FosterErrorCode.INVALID_FOSTER_DATE_RANGE);
    }

    List<FosterStatus> statuses = resolveSearchStatuses(scope, status, activeOnly);

    LocalDateTime startAt = startDate != null ? startDate.atStartOfDay() : null;
    LocalDateTime endAt = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

    Page<Foster> fosterList = fosterRepository.findAllForAdmin(
        statuses,
        includeDeleted,
        startAt,
        endAt,
        pageable
    );

    return PageResponse.of(fosterList, FosterAdminListResponse::from);
  }
    private List<FosterStatus> resolveSearchStatuses(
      FosterManagementScope scope,
      FosterStatus status,
      boolean activeOnly
  ) {
    if (activeOnly && status != null) {
      throw new FosterException(FosterErrorCode.INVALID_FOSTER_SEARCH_CONDITION);
    }

    return switch (scope) {
      case APPLICATION -> {
        if (activeOnly || (status != null && !APPLICATION_STATUSES.contains(status))) {
          throw new FosterException(FosterErrorCode.INVALID_FOSTER_SEARCH_CONDITION);
        }

        yield status == null ? APPLICATION_STATUSES : List.of(status);
      }

      case PROGRESS -> {
        if (activeOnly) {
          yield ACTIVE_FOSTER_STATUSES;
        }

        if (status != null && !PROGRESS_FOSTER_STATUSES.contains(status)) {
          throw new FosterException(FosterErrorCode.INVALID_FOSTER_SEARCH_CONDITION);
        }

        yield status == null ? PROGRESS_FOSTER_STATUSES : List.of(status);
      }
    };
  }

  /**
   * 관리자 임시보호 신청 수정.
   * PATCH 경로지만 전체 상태 전송 방식으로 동작한다.
   */
  @Transactional
  public void updateFoster(
      Long memberId,
      Long fosterId,
      FosterAdminUpdateRequest request
  ) {
    Foster foster = fosterRepository.findById(fosterId)
        .orElseThrow(() -> new FosterException(FosterErrorCode.FOSTER_NOT_FOUND));

    Member reviewer = memberService.getMember(memberId);

    validateFullReplacePayload(foster, request);
    validateFosterPeriod(foster, request);

    foster.updateAdminReview(
        reviewer,
        request.getAnswer(),
        request.getStatus(),
        request.getFosterStartAt(),
        request.getFosterEndAt(),
        request.getFosterExtendAt(),
        request.getFosterCompleteAt()
    );

    syncAnimalFosterStatus(foster);
  }

  /** 관리자 삭제. 삭제 뒤 동물의 임시보호 상태를 재계산한다. */
  @Transactional
  public void deleteFoster(Long fosterId) {
    Foster foster = fosterRepository.findById(fosterId)
        .orElseThrow(() -> new FosterException(FosterErrorCode.FOSTER_NOT_FOUND));

    foster.softDeleteByAdmin();

    syncAnimalFosterStatus(foster);
  }
  
  private void syncAnimalFosterStatus(Foster foster) {
    boolean isFostered = fosterRepository.existsByAnimal_IdAndStatusInAndDeletedAtIsNull(
        foster.getAnimal().getId(),
        ACTIVE_FOSTER_STATUSES
    );

    foster.getAnimal().updateFosteredStatus(isFostered);
  }

  /**
   * 기존 값이 있던 답변·일정을 요청에서 누락하면 거절한다.
   * 관리자 수정은 전체 상태를 전송해야 한다.
   */
  private void validateFullReplacePayload(
      Foster foster,
      FosterAdminUpdateRequest request
  ) {
    boolean omittedExistingAnswer =
        foster.getAnswer() != null && request.getAnswer() == null;

    boolean omittedExistingStartAt =
        foster.getFosterStartAt() != null && request.getFosterStartAt() == null;

    boolean omittedExistingEndAt =
        foster.getFosterEndAt() != null && request.getFosterEndAt() == null;

    boolean omittedExistingExtendAt =
        foster.getFosterExtendAt() != null && request.getFosterExtendAt() == null;

    boolean omittedExistingCompleteAt =
        foster.getFosterCompleteAt() != null && request.getFosterCompleteAt() == null;

    if (
        omittedExistingAnswer ||
        omittedExistingStartAt ||
        omittedExistingEndAt ||
        omittedExistingExtendAt ||
        omittedExistingCompleteAt
    ) {
      throw new FosterException(FosterErrorCode.INCOMPLETE_FOSTER_ADMIN_UPDATE);
    }
  }

  /**
   * 요청 값과 기존 값을 병합해서 일정의 논리적 순서를 검증한다.
   * 실제 종료일은 중간 종료가 가능하므로 연장일과 비교하지 않는다.
   */
  private void validateFosterPeriod(
      Foster foster,
      FosterAdminUpdateRequest request
  ) {
    LocalDateTime fosterStartAt = resolveValue(
        request.getFosterStartAt(),
        foster.getFosterStartAt()
    );
    LocalDateTime fosterEndAt = resolveValue(
        request.getFosterEndAt(),
        foster.getFosterEndAt()
    );
    LocalDateTime fosterExtendAt = resolveValue(
        request.getFosterExtendAt(),
        foster.getFosterExtendAt()
    );
    LocalDateTime fosterCompleteAt = resolveValue(
        request.getFosterCompleteAt(),
        foster.getFosterCompleteAt()
    );

    validateRequiredScheduleByStatus(
        request.getStatus(),
        fosterStartAt,
        fosterEndAt,
        fosterExtendAt,
        fosterCompleteAt
    );

    if (
        fosterStartAt != null &&
        fosterEndAt != null &&
        fosterStartAt.isAfter(fosterEndAt)
    ) {
      throw new FosterException(FosterErrorCode.INVALID_FOSTER_PERIOD);
    }

    if (
        fosterEndAt != null &&
        fosterExtendAt != null &&
        fosterEndAt.isAfter(fosterExtendAt)
    ) {
      throw new FosterException(FosterErrorCode.INVALID_FOSTER_PERIOD);
    }

    if (
        fosterStartAt != null &&
        fosterCompleteAt != null &&
        fosterStartAt.isAfter(fosterCompleteAt)
    ) {
      throw new FosterException(FosterErrorCode.INVALID_FOSTER_PERIOD);
    }
  }

  /**
   * 상태별 일정 필수값 검증.
   * PENDING, REJECTED는 일정 없이 허용한다.
   */
  private void validateRequiredScheduleByStatus(
      FosterStatus status,
      LocalDateTime fosterStartAt,
      LocalDateTime fosterEndAt,
      LocalDateTime fosterExtendAt,
      LocalDateTime fosterCompleteAt
  ) {
    boolean hasBasicSchedule =
        fosterStartAt != null && fosterEndAt != null;

    if (status == FosterStatus.FOSTERING && !hasBasicSchedule) {
      throw new FosterException(FosterErrorCode.REQUIRED_FOSTER_SCHEDULE);
    }

    if (
        status == FosterStatus.EXTENDED &&
        (!hasBasicSchedule || fosterExtendAt == null)
    ) {
      throw new FosterException(FosterErrorCode.REQUIRED_FOSTER_SCHEDULE);
    }

    if (
        status == FosterStatus.ENDED &&
        (!hasBasicSchedule || fosterCompleteAt == null)
    ) {
      throw new FosterException(FosterErrorCode.REQUIRED_FOSTER_SCHEDULE);
    }
  }

  private LocalDateTime resolveValue(
      LocalDateTime requestValue,
      LocalDateTime currentValue
  ) {
    return requestValue != null ? requestValue : currentValue;
  }
}