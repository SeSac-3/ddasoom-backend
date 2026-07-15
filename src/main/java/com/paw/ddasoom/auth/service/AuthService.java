package com.paw.ddasoom.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
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

  @Value("${ddasoom.service-url}")
  private String serviceUrl;

  private final RedisTemplate<String, String> redisTemplate;
  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final MailUtil mailUtil;
  private final RedisTokenService redisTokenService;

  private final SecureRandom random = new SecureRandom();

  private static final Duration AUTH_CODE_TTL = Duration.ofMinutes(3);   // 메일 문구 "3분"과 일치
  private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);   // 인증 후 가입까지 유예

  private static final int MAX_AUTH_CODE_ATTEMPTS = 5;   // 초과 시 코드 폐기, 재발송 요구
  private String authCodeAttemptKey(String email) { return "authCodeAttempt:" + email; }

  private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);  // 메일 안내 문구와 일치시킬 것
  private String cooldownKey(String email) { return "authCodeCooldown:" + email; }

  // 키 설계
  private String authCodeKey(String email) { return "authCode:" + email; }
  private String verifiedKey(String email) { return "verified:" + email; }

  /**-------------------------------------------------------------------------------------------------
   * 회원가입 요청 중 , 이메일 인증 로직
   * -------------------------------------------------------------------------------------------------*/

  /** 인증 코드 발송 (재발송 겸용) */
  public void sendAuthCode(String email) {
    if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey(email)))) {
      throw new AuthException(AuthErrorCode.AUTH_CODE_COOLDOWN);
    }
    if (memberRepository.existsByEmail(email)) {
      throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
    }

    redisTemplate.delete(verifiedKey(email));
    redisTemplate.delete(authCodeAttemptKey(email));   // A-6 연동 — 재발송은 새 시도 기회

    String code = String.format("%06d", random.nextInt(1_000_000));
    redisTemplate.opsForValue().set(authCodeKey(email), code, AUTH_CODE_TTL);

    mailUtil.sendAuthCodeEmail(email, code);   // 발송 실패 시 여기서 throw — 쿨다운은 미기록
    redisTemplate.opsForValue().set(cooldownKey(email), "sent", RESEND_COOLDOWN);  // 발송 성공 후에만
  }

  /** 인증 코드 검증 → 성공 시 인증 완료 플래그 생성 */
  public void verifyAuthCode(String email, String code) {
    String storedCode = redisTemplate.opsForValue().get(authCodeKey(email));

    boolean isCodeMismatch = storedCode == null || !storedCode.equals(code);
    if (isCodeMismatch) {
      // 실패 카운트 — 코드와 같은 수명으로 만료 (INCR 후 첫 증가 시 TTL 부여)
      Long attempts = redisTemplate.opsForValue().increment(authCodeAttemptKey(email));
      if (attempts != null && attempts == 1) {
        redisTemplate.expire(authCodeAttemptKey(email), AUTH_CODE_TTL);
      }
      // 5회 초과 → 브루트포스 간주, 코드 자체를 폐기 (재발송부터 다시)
      if (attempts != null && attempts >= MAX_AUTH_CODE_ATTEMPTS) {
        redisTemplate.delete(authCodeKey(email));
      }
      throw new AuthException(AuthErrorCode.INVALID_AUTH_CODE);
    }

    redisTemplate.delete(authCodeKey(email));
    redisTemplate.delete(authCodeAttemptKey(email));   // 성공 시 카운터도 정리
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

    /**-------------------------------------------------------------------------------------------------
   *  비밀번호 재설정 메일 발송.
   * -------------------------------------------------------------------------------------------------*/
    public void sendPasswordResetLink(String email) {
        memberRepository.findByEmail(email)
                .filter(member -> !member.isDeleted())
                .filter(member -> member.getPassword() != null)   // 소셜 전용 회원은 재설정 대상 아님
                .ifPresent(member -> {
                    String token = UUID.randomUUID().toString();
                    redisTokenService.saveResetToken(token, member.getId());

                    String resetLink = serviceUrl + "/reset-password?token=" + token;
                    mailUtil.sendPasswordResetEmail(email, resetLink);
                });
        // 대상이 없어도 예외 없이 정상 종료 → 컨트롤러는 항상 동일 응답
    }

  /**-------------------------------------------------------------------------------------------------
   *   토큰으로 비밀번호 재설정 → 성공 시 전 세션 무효화
   * -------------------------------------------------------------------------------------------------*/
    @Transactional
    public void resetPassword(String token, String newPassword) {
        Long memberId = redisTokenService.findMemberIdByResetToken(token);
        if (memberId == null) {
            throw new AuthException(AuthErrorCode.INVALID_RESET_TOKEN);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_RESET_TOKEN));

        // 재설정 링크 발급 후 탈퇴한 계정 — 발송 단계(sendPasswordResetLink)와 동일 기준으로 차단.
        // 탈퇴 사실을 노출하지 않기 위해 전용 코드 대신 "무효 토큰"과 동일 응답으로 합친다.
        if (member.isDeleted()) {
        throw new AuthException(AuthErrorCode.INVALID_RESET_TOKEN);
        }

        member.changePassword(passwordEncoder.encode(newPassword));

        redisTokenService.deleteResetToken(token);          // 일회용 — 재사용 차단
        redisTokenService.deleteRefreshTokens(memberId);    // 자격증명 교체 → 기존 세션 무효화
    }

    /** 닉네임 사용 가능 여부 — 사용 가능하면 true */
    public boolean isNicknameAvailable(String nickname) {
        return !memberRepository.existsByNickname(nickname);
    }

}
