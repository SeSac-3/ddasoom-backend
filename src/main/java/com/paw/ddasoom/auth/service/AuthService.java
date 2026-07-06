package com.paw.ddasoom.auth.service;

import java.security.SecureRandom;
import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.auth.dto.request.SignupRequest;
import com.paw.ddasoom.auth.dto.response.SignupResponse;
import com.paw.ddasoom.auth.exception.AuthErrorCode;
import com.paw.ddasoom.auth.exception.AuthException;
import com.paw.ddasoom.auth.util.MailUtil;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private final RedisTemplate<String, String> redisTemplate;
  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final MailUtil mailUtil;

  private final SecureRandom random = new SecureRandom();

  private static final Duration AUTH_CODE_TTL = Duration.ofMinutes(3);   // 메일 문구 "3분"과 일치
  private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);   // 인증 후 가입까지 유예

  // 키 설계
  private String authCodeKey(String email) { return "authCode:" + email; }
  private String verifiedKey(String email) { return "verified:" + email; }

  /**-------------------------------------------------------------------------------------------------
   * 회원가입 요청 중 , 이메일 인증 로직
   * -------------------------------------------------------------------------------------------------*/

  /** 인증 코드 발송 (재발송 겸용) */
  public void sendAuthCode(String email) {
    // 이미 가입된 이메일이면 코드 발송 자체를 차단
    if (memberRepository.existsByEmail(email)) {
        throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
    }

    // 재인증 요청 대비: 기존 인증 완료 상태를 무효화
    redisTemplate.delete(verifiedKey(email));

    String code = String.format("%06d", random.nextInt(1_000_000));
    redisTemplate.opsForValue().set(authCodeKey(email), code, AUTH_CODE_TTL);

    mailUtil.sendAuthCodeEmail(email, code);
    log.debug("인증 코드 발송 완료 - email: {}", email);
  }

  /** 인증 코드 검증 → 성공 시 인증 완료 플래그 생성 */
  public void verifyAuthCode(String email, String code) {
    String storedCode = redisTemplate.opsForValue().get(authCodeKey(email));

    // 만료(null)와 불일치를 같은 예외로 처리
    if (storedCode == null || !storedCode.equals(code)) {
        throw new AuthException(AuthErrorCode.INVALID_AUTH_CODE);
    }

    redisTemplate.delete(authCodeKey(email));
    redisTemplate.opsForValue().set(verifiedKey(email), "true", VERIFIED_TTL);
  }

  /**-------------------------------------------------------------------------------------------------
   * 일반(아이디/비밀번호) 회원가입 요청
   * -------------------------------------------------------------------------------------------------*/
  @Transactional
  public SignupResponse signup(SignupRequest request) {
    // 1. 이메일 인증 완료 여부 확인 (30분 초과 시 재인증 필요)
    if (redisTemplate.opsForValue().get(verifiedKey(request.getEmail())) == null) {
        throw new AuthException(AuthErrorCode.EMAIL_NOT_VERIFIED);
    }

    // 2. 중복 재확인 (인증~가입 사이 선점 가능성)
    if (memberRepository.existsByEmail(request.getEmail())) {
        throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
    }
    if (memberRepository.existsByNickname(request.getNickname())) {
        throw new AuthException(AuthErrorCode.NICKNAME_ALREADY_EXISTS);
    }

    // 3. 저장 (비밀번호 인코딩은 toEntity에 위임)
    Member member = memberRepository.save(request.toEntity(passwordEncoder));

    // 4. 인증 플래그 소진
    redisTemplate.delete(verifiedKey(request.getEmail()));

    // 5. 환영 메일 — 실패해도 가입은 성공해야 하므로 로그만 남김
    try {
        mailUtil.sendWelcomeEmail(member.getEmail());
    } catch (AuthException e) {
        log.warn("환영 메일 발송 실패 (가입은 정상 처리) - email: {}", member.getEmail());
    }

    return SignupResponse.from(member);
  }

}
