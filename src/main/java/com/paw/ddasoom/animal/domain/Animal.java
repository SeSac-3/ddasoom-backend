package com.paw.ddasoom.animal.domain;

import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.paw.ddasoom.animal.exception.AnimalErrorCode;
import com.paw.ddasoom.animal.exception.AnimalException;
import com.paw.ddasoom.common.util.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "animal",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_animal_abandonment", columnNames = "abandonment_id")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class Animal extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "animal_id")
  private Long id;

  @Column(name = "abandonment_id", nullable = false, length = 20)
  private String abandonmentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false, length = 20)
  private AnimalKind kind; // 상위 품종 분류 (개/고양이 등)

  @Column(name = "nickname", nullable = false, length = 50)
  @Builder.Default
  private String nickname = "미정";

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "gender", nullable = false, length = 1)
  private AnimalGender gender = AnimalGender.Q;

  @Column(name = "type_name", nullable = false, length = 50)
  private String typeName; // 품종 이름

  @Column(name = "age", nullable = false, length = 20)
  private String age; // 출생 연도 (예: 2026(년도))

  @Column(name = "location", nullable = false, length = 100)
  private String location;

  @Column(name = "weight", nullable = false, length = 20)
  private String weight; // 몸무게 (kg)

  @Column(name = "color", nullable = false, length = 50)
  private String color;

  @Builder.Default
  @Column(name = "special_mark", length = 255)
  private String specialMark = "없음";

  @Builder.Default
  @Column(name = "vaccination_chk", length = 50)
  private String vaccinationChk = "접종 안함";

  @Column(name = "image_url", length = 255)
  private String imageUrl;

  @Builder.Default
  @Column(name = "like_count", nullable = false, columnDefinition = "INT UNSIGNED DEFAULT 0")
  private int likeCount = 0; // 캐시 컬럼 - animal_like 기준 동기화

  @Builder.Default
  @Column(name = "is_fostered", nullable = false)
  private boolean isFostered = false; // foster 기준 동기화

  @Column(name = "rescued_at", columnDefinition = "DATETIME(6)")
  private LocalDateTime rescuedAt; // 서비스 로직에서 명시적 세팅, 자동화 금지

  public void markAsFostered() {
    this.isFostered = true;
  }

  public void changeNickname(String nickname) {
    if (nickname == null || nickname.isBlank()) {
      throw new AnimalException(AnimalErrorCode.ANIMAL_NAME_INVALID);
    }
    this.nickname = nickname;
  }

  // Animal.java에 추가할 비즈니스 메서드

  // 1. API 동기화 배치 전용 — likeCount/isFostered/deletedAt은 건드리지 않음
  public void updateFromApi(
    String kind,
    String gender,
    String typeName,
    String age,
    String location,
    String weight,
    String color,
    String specialMark,
    String vaccinationChk,
    String imageUrl,
    LocalDateTime rescuedAt) {
    this.kind = AnimalKind.from(kind);
    this.gender = AnimalGender.from(gender);
    this.typeName = typeName;
    this.age = age;
    this.location = location;
    this.weight = weight;
    this.color = color;
    this.specialMark = StringUtils.defaultIfBlank(specialMark, "없음");
    this.vaccinationChk = StringUtils.defaultIfBlank(vaccinationChk, "접종 안함");
    this.imageUrl = imageUrl;
    this.rescuedAt = rescuedAt;
  }

  // 2. foster 도메인 전용 — 다른 필드는 건드리지 않음
  public void updateFosteredStatus(boolean isFostered) {
    this.isFostered = isFostered;
  }

}
