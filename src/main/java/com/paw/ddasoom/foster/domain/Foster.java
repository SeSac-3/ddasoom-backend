package com.paw.ddasoom.foster.domain;

import com.paw.ddasoom.common.util.BaseTimeEntity;
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
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

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
  @Column(name = "foster_num", nullable = false, updatable = false, length = 36, columnDefinition = "CHAR(36)")
  private UUID fosterNum;

  @Column(name = "age", nullable = false, length = 10)
  private String age;

  @Column(name = "job", nullable = false, length = 30)
  private String job;

  @Column(name = "message", columnDefinition = "TEXT")
  private String message;

  @Column(name = "answer", columnDefinition = "TEXT")
  private String answer;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(Types.VARCHAR)
  @Column(name = "status", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
  private FosterStatus status;

  @Column(name = "foster_start_at", columnDefinition = "DATETIME(6)")
  private LocalDateTime fostertartAt;

  @Column(name = "foster_end_at", columnDefinition = "DATETIME(6)")
  private LocalDateTime fosterEndAt;

  @Column(name = "foster_extend_at", columnDefinition = "DATETIME(6)")
  private LocalDateTime fosterExtendAt;

  @Column(name = "foster_complete_at", columnDefinition = "DATETIME(6)")
  private LocalDateTime fosterCompleteAt;

  @Column(name = "deleted_at", columnDefinition = "DATETIME(6)")
  private LocalDateTime deletedAt;

}