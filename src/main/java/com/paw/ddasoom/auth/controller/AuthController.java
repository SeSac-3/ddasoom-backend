package com.paw.ddasoom.auth.controller;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.auth.dto.request.AuthCodeSendRequest;
import com.paw.ddasoom.auth.dto.request.AuthCodeVerifyRequest;
import com.paw.ddasoom.auth.dto.request.LoginRequest;
import com.paw.ddasoom.auth.dto.request.PasswordResetEmailRequest;
import com.paw.ddasoom.auth.dto.request.PasswordResetRequest;
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

    /** 이메일 인증 코드 발송 (재발송 겸용) — IP 단위 시간당 제한 적용 (AUTH_007) */
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendAuthCode(
            @Valid @RequestBody AuthCodeSendRequest request,
            HttpServletRequest httpRequest) {
        authService.sendAuthCode(request.getEmail(), resolveClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("인증 코드가 발송되었습니다."));
    }

    /**
     * 클라이언트 IP 추출 — 리버스 프록시(운영 Nginx 등) 경유 시 X-Forwarded-For의 첫 값이 원 IP.
     * 로컬/데모(프록시 없음)는 헤더가 없어 getRemoteAddr() 사용.
     * ⚠️ XFF는 클라이언트가 위조 가능한 헤더 — rate limit 용도(우회 시 피해가 "메일 몇 통 더")라 수용,
     *    인증·인가 판단에는 절대 사용하지 말 것.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();   // "client, proxy1, proxy2" 형식의 첫 토큰
        }
        return request.getRemoteAddr();
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

    /** 비밀번호 재설정 메일 발송 — 이메일 존재 여부와 무관하게 동일 응답 */
    @PostMapping("/password/reset-request")
    public ResponseEntity<ApiResponse<Void>> sendPasswordResetLink(
            @Valid @RequestBody PasswordResetEmailRequest request) {
        authService.sendPasswordResetLink(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
                "비밀번호 재설정 메일을 발송했습니다. 메일함을 확인해 주세요."));
    }

    /** 토큰으로 비밀번호 재설정 — 성공 시 재로그인 필요 */
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 재설정되었습니다. 다시 로그인해 주세요."));
    }

    /**
     * 닉네임 중복 확인 (회원가입/추가정보 폼 실시간 검증용).
     * 사용 가능 = true. 이메일 중복 확인은 열거 공격 방지를 위해 제공하지 않음.
     * @Vaild 어노테이션은 사용하지 않음. 
     *    -> 중복확인의 경우 , 형식이 틀린 닉네임은 어차피 DB에 없음. 
     *    -> 형식 검증은 최종 제출의 @Pattern 이 담당하므로 중복역할 제거.
     */
    @GetMapping("/nickname/available")
    public ResponseEntity<ApiResponse<Boolean>> checkNicknameAvailable(
            @RequestParam("nickname") String nickname) {
        boolean available = authService.isNicknameAvailable(nickname);
        return ResponseEntity.ok(ApiResponse.success(available));
    }

}
