package com.paw.ddasoom.common.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paw.ddasoom.auth.exception.AuthErrorCode;
import com.paw.ddasoom.common.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/** 미인증(401) 응답을 ApiResponse 포맷으로 통일 
 * — 필터 레벨은 GlobalExceptionHandler가 못 잡으므로 필수 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
      AuthErrorCode errorCode = AuthErrorCode.UNAUTHORIZED;
      response.setStatus(errorCode.getStatus().value());
      response.setContentType("application/json;charset=UTF-8");
      objectMapper.writeValue(response.getWriter(),
              ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
  }
  
}
