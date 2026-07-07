package com.paw.ddasoom.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paw.ddasoom.auth.domain.LoginLog;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
  // 조회 메서드는 관리자/마이페이지 기능 구현 시 추가 예정

}
