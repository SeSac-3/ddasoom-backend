package com.paw.ddasoom.foster.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.animal.exception.AnimalErrorCode;
import com.paw.ddasoom.animal.exception.AnimalException;
import com.paw.ddasoom.animal.repository.AnimalRepository;
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

  /**
   * 임시보호 신청 관리 화면에서 조회하는 상태 목록이다.
   * 임시보호신청관리(신청대기,신청거절)
   */
  private static final List<FosterStatus> APPLICATION_STATUSES = List.of(
      FosterStatus.PENDING,
      FosterStatus.REJECTED
  );

  /**
   * 임시보호 진행 관리 화면에서 조회하는 상태 목록이다.
   * 임시보호진행관리(임시보호중[임시보호중,임시보호연장],종료)
   */
  private static final List<FosterStatus> PROGRESS_FOSTER_STATUSES = List.of(
      FosterStatus.FOSTERING,
      FosterStatus.EXTENDED,
      FosterStatus.ENDED
  );

  /**
   * 동물의 isFostered 값을 true로 만드는 활성 임시보호 상태 목록이다.
   */
  private static final List<FosterStatus> ACTIVE_FOSTER_STATUSES = List.of(
      FosterStatus.FOSTERING,
      FosterStatus.EXTENDED
  );

  private final FosterRepository fosterRepository;
  private final AnimalRepository animalRepository;
  private final MemberService memberService;

  /**
   * 관리자 임시보호 신청 상세를 조회한다.
   *
   * <p>관리자는 삭제된 신청도 조회할 수 있다.</p>
   *
   * @param fosterId 임시보호 신청 PK
   * @return 관리자용 상세 응답
   */
  @Transactional(readOnly = true)
  public FosterAdminDetailResponse getFosterDetail(Long fosterId) {
    Foster foster = fosterRepository.findById(fosterId)
        .orElseThrow(() -> new FosterException(FosterErrorCode.FOSTER_NOT_FOUND));

    return FosterAdminDetailResponse.from(foster);
  }

  /**
   * 관리자 임시보호 신청 목록을 조회한다.
   *
   * @param scope 신청 관리 또는 진행 관리 범위
   * @param status 단일 상태 필터
   * @param activeOnly 활성 임시보호 상태만 조회할지 여부
   * @param includeDeleted 삭제된 신청 포함 여부
   * @param startDate 신청일 조회 시작일
   * @param endDate 신청일 조회 종료일
   * @param pageable 페이지 정보
   * @return 관리자용 목록 응답
   */
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

  /**
   * 관리 범위와 상태 필터 조합을 검증하고 실제 조회할 상태 목록을 결정한다.
   */
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
   * 관리자 임시보호 신청 처리 정보를 수정한다.
   *
   * <p>PATCH 경로지만 답변과 일정은 전체 상태 전송 방식으로 동작한다.</p>
   *
   * @param memberId 현재 로그인한 관리자 PK
   * @param fosterId 수정할 임시보호 신청 PK
   * @param request 관리자 수정 요청
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
    validateNoOtherActiveFoster(foster, request.getStatus());

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

  /**
   * 관리자 권한으로 임시보호 신청을 소프트 삭제하고 동물 보호 상태를 재계산한다.
   */
  @Transactional
  public void deleteFoster(Long fosterId) {
    Foster foster = fosterRepository.findById(fosterId)
        .orElseThrow(() -> new FosterException(FosterErrorCode.FOSTER_NOT_FOUND));

    foster.softDeleteByAdmin();

    syncAnimalFosterStatus(foster);
  }

  /**
   * 활성 상태 전환 전에 동물 행을 잠근 뒤 다른 활성 신청 존재 여부를 확인한다.
   *
   * <p>같은 동물에 대해 여러 관리자가 동시에 승인하더라도 한 신청만 활성 상태가 된다.</p>
   *
   * @param foster 현재 수정 중인 신청
   * @param nextStatus 변경할 상태
   */
  private void validateNoOtherActiveFoster(
      Foster foster,
      FosterStatus nextStatus
  ) {
    boolean willBeActive =
        nextStatus == FosterStatus.FOSTERING ||
        nextStatus == FosterStatus.EXTENDED;

    if (!willBeActive) {
      return;
    }

    Animal animal = animalRepository.findByIdForUpdate(foster.getAnimal().getId())
        .orElseThrow(() -> new AnimalException(AnimalErrorCode.ANIMAL_NOT_FOUND));

    boolean hasOtherActiveFoster =
        fosterRepository.existsByAnimal_IdAndIdNotAndStatusInAndDeletedAtIsNull(
            animal.getId(),
            foster.getId(),
            ACTIVE_FOSTER_STATUSES
        );

    if (hasOtherActiveFoster) {
      throw new FosterException(FosterErrorCode.DUPLICATE_ACTIVE_FOSTER);
    }
  }

  /**
   * 해당 동물의 삭제되지 않은 활성 신청 존재 여부로 isFostered를 동기화한다.
   */
  private void syncAnimalFosterStatus(Foster foster) {
    boolean isFostered = fosterRepository.existsByAnimal_IdAndStatusInAndDeletedAtIsNull(
        foster.getAnimal().getId(),
        ACTIVE_FOSTER_STATUSES
    );

    foster.getAnimal().updateFosteredStatus(isFostered);
  }

  /**
   * 기존에 값이 있던 답변·일정 필드를 요청에서 누락했는지 검증한다.
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
   * 요청 값과 기존 값을 병합해 임시보호 일정의 논리적 순서를 검증한다.
   *
   * <p>중간 종료는 가능하므로 완료일과 연장일은 비교하지 않는다.</p>
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

    if (fosterStartAt != null && fosterEndAt != null && fosterStartAt.isAfter(fosterEndAt)) {
      throw new FosterException(FosterErrorCode.INVALID_FOSTER_PERIOD);
    }

    if (
    fosterEndAt != null &&
    fosterExtendAt != null &&
    !fosterExtendAt.isAfter(fosterEndAt)
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
   * 상태별 필수 일정 정보를 검증한다.
   *
   * <p>PENDING, REJECTED 상태는 일정 없이 허용한다.</p>
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

  /**
   * 요청 값이 있으면 요청 값을, 없으면 기존 값을 사용한다.
   */
  private LocalDateTime resolveValue(
      LocalDateTime requestValue,
      LocalDateTime currentValue
  ) {
    return requestValue != null ? requestValue : currentValue;
  }
}