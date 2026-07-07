package com.paw.ddasoom.auth.domain;

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
@Table(name = "login_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "login_log_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false,
          foreignKey = @ForeignKey(name = "fk_login_logs_member"))
  private Member member;

  @Enumerated(EnumType.STRING)
  @Column(name = "login_type", nullable = false, length = 20)
  private LoginType loginType;

  @Builder
  public LoginLog(Member member, LoginType loginType) {
      this.member = member;
      this.loginType = loginType;
  }
}
