package com.paw.ddasoom.auth.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.paw.ddasoom.auth.domain.LoginLog;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
  /** 관리자 회원 상세 — 최근 로그인 이력 5건 미리보기 */
  List<LoginLog> findTop5ByMemberIdOrderByCreatedAtDesc(Long memberId);

  /** 관리자 회원 상세 — 로그인 이력 전체 (페이징) */
  Page<LoginLog> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
}
