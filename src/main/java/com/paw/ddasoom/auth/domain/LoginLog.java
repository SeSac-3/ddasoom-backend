package com.paw.ddasoom.auth.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 로그인 이력 — 불변 로그 (ip/userAgent는 보안 감사 기능 도입 시 추가) */
@Entity
@Table(name = "login_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginLog{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "login_log_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false,
          foreignKey = @ForeignKey(name = "fk_login_log_member"))
  private Member member;

  @Enumerated(EnumType.STRING)
  @Column(name = "login_type", nullable = false, length = 20)
  private LoginType loginType;

  // 로그인 시각 = DB DEFAULT가 기록 (append-only 로그 — updated_at 없음, DB 컨벤션 예외 조항)
  @Generated(event = EventType.INSERT)
  @Column(insertable = false, updatable = false, columnDefinition = "DATETIME(6)")
  private LocalDateTime createdAt;

  @Builder
  public LoginLog(Member member, LoginType loginType) {
      this.member = member;
      this.loginType = loginType;
  }
}
