package com.paw.ddasoom.common.security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paw.ddasoom.auth.exception.AuthErrorCode;
import com.paw.ddasoom.common.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/** 권한 부족(403) 응답 통일 — GUEST의 USER 전용 API 접근 등 */
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
  private final ObjectMapper objectMapper;

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException {
      AuthErrorCode errorCode = AuthErrorCode.FORBIDDEN;
      response.setStatus(errorCode.getStatus().value());
      response.setContentType("application/json;charset=UTF-8");
      objectMapper.writeValue(response.getWriter(),
              ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
  }
}
