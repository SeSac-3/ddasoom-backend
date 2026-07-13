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

  private Foster(
      Animal animal,
      Member user,
      String age,
      String job,
      String message) {
    this.animal = animal;
    this.user = user;
    this.fosterNum = UUID.randomUUID();
    this.age = age;
    this.job = job;
    this.message = message;
    this.status = FosterStatus.PENDING;
  }

  public static Foster create(
      Animal animal,
      Member user,
      String age,
      String job,
      String message) {
    return new Foster(
        animal,
        user,
        age,
        job,
        message);
  }

  public void softDelete() {
    if(this.deletedAt != null){
      throw new FosterException(FosterErrorCode.ALREADY_DELETED_FOSTER);
    }
    this.deletedAt = LocalDateTime.now();
  }

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

}
