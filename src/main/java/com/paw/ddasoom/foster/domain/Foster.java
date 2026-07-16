package com.paw.ddasoom.foster.domain;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.common.util.BaseTimeEntity;
import com.paw.ddasoom.foster.exception.FosterErrorCode;
import com.paw.ddasoom.foster.exception.FosterException;
import com.paw.ddasoom.member.domain.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "foster", uniqueConstraints = {
    @UniqueConstraint(name = "uk_foster_foster_num", columnNames = "foster_num")
})
public class Foster extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "foster_id", nullable = false)
  private Long fosterId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "animal_id", nullable = false, foreignKey = @ForeignKey(name = "fk_foster_animal"))
  private Animal animal;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_foster_user"))
  private Member user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewer_id", foreignKey = @ForeignKey(name = "fk_foster_reviewer"))
  private Member reviewer;

  @JdbcTypeCode(Types.CHAR)
  @Column(nullable = false, updatable = false, length = 36, columnDefinition = "CHAR(36)")
  private UUID fosterNum;

  @Column(nullable = false, length = 10)
  private String age;

  @Column(nullable = false, length = 30)
  private String job;

  @Column(columnDefinition = "TEXT")
  private String message;

  @Column(columnDefinition = "TEXT")
  private String answer;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(Types.VARCHAR)
  @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
  private FosterStatus status = FosterStatus.PENDING;

  @Column(columnDefinition = "DATETIME(6)")
  private LocalDateTime fosterStartAt;

  @Column(columnDefinition = "DATETIME(6)")
  private LocalDateTime fosterEndAt;

  @Column(columnDefinition = "DATETIME(6)")
  private LocalDateTime fosterExtendAt;

  @Column(columnDefinition = "DATETIME(6)")
  private LocalDateTime fosterCompleteAt;

  @Column(columnDefinition = "DATETIME(6)")
  private LocalDateTime deletedAt;
  // 임시보호신청 엔티티 초기화 - UUID 발급 및 기본 상태(PENDING) 설정
  @Builder
  private Foster(
      Animal animal,
      Member user,
      String age,
      String job,
      String message
    ) {
    this.animal = animal;
    this.user = user;
    this.fosterNum = UUID.randomUUID();
    this.age = age;
    this.job = job;
    this.message = message;
    this.status = FosterStatus.PENDING;
}

  // 리치도메인 메서드 -> 임시보호신청 soft delete 처리(deletedAt 기준 삭제)
  public void softDelete() {
    if(this.deletedAt != null){
      throw new FosterException(FosterErrorCode.ALREADY_DELETED_FOSTER);
    }
    this.deletedAt = LocalDateTime.now();
  }
  // 리치도메인 메서드 -> 유저 신청 내용 수정 (PENDING 상태에서 age/job/message만 수정)
  public void updateUserRequest(String age, String job, String message) {
    if (this.deletedAt != null) {
      throw new FosterException(FosterErrorCode.ALREADY_DELETED_FOSTER);
    }

    if (this.status != FosterStatus.PENDING) {
      throw new FosterException(FosterErrorCode.INVALID_FOSTER_STATUS);
    }
    this.age = age;
    this.job = job;
    this.message = message;
  }

  // 관리자 신청 처리 정보 수정 (검토자/답변/상태/임시보호 일정 변경)
  public void updateAdminReview(
    Member reviewer,
    String answer,
    FosterStatus status,
    LocalDateTime fosterStartAt,
    LocalDateTime fosterEndAt,
    LocalDateTime fosterExtendAt,
    LocalDateTime fosterCompleteAt
  ){
    if (this.deletedAt != null){
      throw new FosterException(FosterErrorCode.ALREADY_DELETED_FOSTER);
    }
    // 상태 변경 불가 검증
    validateStatusTransition(status);
    this.reviewer = reviewer;
    this.answer = answer;
    this.status = status;
    this.fosterStartAt = fosterStartAt;
    this.fosterEndAt = fosterEndAt;
    this.fosterExtendAt = fosterExtendAt;
    this.fosterCompleteAt = fosterCompleteAt;
  }

  // 관리자 상태 변경 시 허용되지 않은 상태 전이를 방지하는 검증 메서드
  private void validateStatusTransition(FosterStatus nextStatus){
    // 업데이트시 동일한 상태일시 처리 (PENDING == PENDING)
    if (this.status == nextStatus) {
      return;
    }

    boolean isValid = switch (this.status) {
      case PENDING -> nextStatus == FosterStatus.FOSTERING || nextStatus == FosterStatus.REJECTED;
      case FOSTERING -> nextStatus == FosterStatus.EXTENDED || nextStatus == FosterStatus.ENDED;
      case EXTENDED -> nextStatus == FosterStatus.ENDED;
      case REJECTED, ENDED -> false;
    };

    if (!isValid) {
      throw new FosterException(FosterErrorCode.INVALID_FOSTER_STATUS_TRANSITION);
    }
  }
}
