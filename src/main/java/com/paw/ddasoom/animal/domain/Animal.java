package com.paw.ddasoom.animal.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.Check;

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
@Check(name = "chk_animal_weight", constraints = "weight > 0")
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
  @Column(name = "gender", nullable = false, length = 20)
  private AnimalGender gender = AnimalGender.Q;

  @Column(name = "type_name", nullable = false, length = 50)
  private String typeName; // 품종 이름

  @Column(name = "age", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
  private int age; // 출생 연도 (예: 2026) - 공공API 원본 파싱 결과

  @Column(name = "raw_age", length = 50)
  private String rawAge; // 나이 원본 문자열 (공공API 그대로, 파싱 실패 추적용)

  @Column(name = "location", nullable = false, length = 100)
  private String location;

  @Column(name = "weight", nullable = false, precision = 5, scale = 2)
  private int weight; // 몸무게 (kg) - 공공API 원본 파싱 결과

  @Column(name = "raw_weight", length = 50)
  private String rawWeight; // 몸무게 원본 문자열 (공공API 그대로, 파싱 실패 추적용)

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

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
