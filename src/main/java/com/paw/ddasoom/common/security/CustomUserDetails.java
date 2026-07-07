package com.paw.ddasoom.common.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.paw.ddasoom.member.domain.Role;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails{
  private final Long memberId;
  private final Role role;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
      // hasRole("USER") 매칭을 위한 ROLE_ 접두사
      return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getUsername() {
      return String.valueOf(memberId);
  }

  @Override
  public String getPassword() {
      return null; // 토큰 기반 인증 — 비밀번호 미보유
  }

}
