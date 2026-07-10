package com.paw.ddasoom.member.dto.response;

import java.time.LocalDateTime;

import com.paw.ddasoom.auth.domain.LoginLog;
import com.paw.ddasoom.auth.domain.LoginType;

import lombok.Builder;
import lombok.Getter;

/** 로그인 이력 행 (관리자 상세 미리보기 + 이력 페이징 공용) */
@Getter
@Builder
public class LoginLogResponse {
  private Long loginLogId;
  private LoginType loginType;
  private LocalDateTime loginAt;   // createdAt의 의미 있는 이름 (불변 로그: 생성 = 로그인 시각)

  public static LoginLogResponse from(LoginLog loginLog) {
      return LoginLogResponse.builder()
              .loginLogId(loginLog.getId())
              .loginType(loginLog.getLoginType())
              .loginAt(loginLog.getCreatedAt())
              .build();
  }
}
