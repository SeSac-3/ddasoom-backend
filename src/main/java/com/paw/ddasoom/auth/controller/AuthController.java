package com.paw.ddasoom.auth.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.auth.dto.request.AuthCodeSendRequest;
import com.paw.ddasoom.auth.dto.request.AuthCodeVerifyRequest;
import com.paw.ddasoom.auth.dto.request.SignupRequest;
import com.paw.ddasoom.auth.dto.response.SignupResponse;
import com.paw.ddasoom.auth.service.AuthService;
import com.paw.ddasoom.common.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
private final AuthService authService;

  /** 이메일 인증 코드 발송 (재발송 겸용) */
  @PostMapping("/email/send")
  public ResponseEntity<ApiResponse<Void>> sendAuthCode(@Valid @RequestBody AuthCodeSendRequest request) {
      authService.sendAuthCode(request.getEmail());
      return ResponseEntity.ok(ApiResponse.success("인증 코드가 발송되었습니다."));
  }

  /** 이메일 인증 코드 검증 */
  @PostMapping("/email/verify")
  public ResponseEntity<ApiResponse<Void>> verifyAuthCode(@Valid @RequestBody AuthCodeVerifyRequest request) {
      authService.verifyAuthCode(request.getEmail(), request.getCode());
      return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다."));
  }

  /** 일반 회원가입 */
  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
      SignupResponse response = authService.signup(request);
      return ResponseEntity
              .status(HttpStatus.CREATED)
              .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
  }


}
