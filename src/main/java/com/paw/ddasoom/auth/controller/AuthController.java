package com.paw.ddasoom.auth.controller;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.auth.dto.request.AuthCodeSendRequest;
import com.paw.ddasoom.auth.dto.request.AuthCodeVerifyRequest;
import com.paw.ddasoom.auth.dto.request.LoginRequest;
import com.paw.ddasoom.auth.dto.request.SignupRequest;
import com.paw.ddasoom.auth.dto.response.LoginResponse;
import com.paw.ddasoom.auth.dto.response.SignupResponse;
import com.paw.ddasoom.auth.exception.AuthErrorCode;
import com.paw.ddasoom.auth.exception.AuthException;
import com.paw.ddasoom.auth.service.AuthService;
import com.paw.ddasoom.auth.service.LoginService;
import com.paw.ddasoom.auth.util.CookieUtil;
import com.paw.ddasoom.auth.util.JwtUtil;
import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginService loginService;
    private final CookieUtil cookieUtil;
    private final JwtUtil jwtUtil;

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

    /** 로그인 — AT는 body, RT는 HttpOnly 쿠키 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginService.LoginResult result = loginService.login(request);
        ResponseCookie refreshCookie = cookieUtil.createRefreshCookie(
                    result.refreshToken(), Duration.ofMillis(jwtUtil.getRefreshTokenValidity()));

        return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(ApiResponse.success("로그인 되었습니다.", result.response()));
    }

    /** 토큰 재발급 — RT 로테이션. grace 통과 시(refreshToken == null) 쿠키 미갱신 */
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<LoginResponse>> reissue(HttpServletRequest request) {
        String refreshToken = cookieUtil.findRefreshToken(request)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        LoginService.LoginResult result = loginService.reissue(refreshToken);

        if (result.refreshToken() == null) {
            return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", result.response()));
        }
        ResponseCookie refreshCookie = cookieUtil.createRefreshCookie(
                result.refreshToken(), Duration.ofMillis(jwtUtil.getRefreshTokenValidity()));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success("토큰이 재발급되었습니다.", result.response()));
    }

    /** 로그아웃 — 인증 필수 (SecurityConfig에서 authenticated) */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader("Authorization") String authorizationHeader) {

        String accessToken = authorizationHeader.replaceFirst("^Bearer ", "");
        loginService.logout(userDetails.getMemberId(), accessToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.deleteRefreshCookie().toString())
                .body(ApiResponse.success("로그아웃 되었습니다."));
    }


}
