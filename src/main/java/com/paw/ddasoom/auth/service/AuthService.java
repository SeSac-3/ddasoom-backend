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

/**
 * 회원가입 흐름(이메일 인증 → 가입)과 비밀번호 재설정을 담당하는 서비스.
 *
 * <p>로그인 이후의 세션 관리는 LoginService, 이 클래스는 "계정을 만들기 전/비밀번호를 되찾기"까지의
 * 인증 절차를 맡는다. 이메일 인증·쿨다운·시도제한·재설정 토큰 등 상태는 대부분 Redis에 TTL로 얹어
 * 별도 배치 없이 자연 만료되게 설계했다.
 *
 * <p>공통 방어 원칙:
 * ① 열거 공격 방지 — 비밀번호 재설정은 이메일 존재 여부와 무관하게 항상 동일 응답.
 * ② 다층 발송 제한 — 이메일 쿨다운(60초) + IP 시간당 제한으로 대량 발송 악용 차단.
 * ③ 외부 I/O(메일) 실패가 핵심 트랜잭션(가입)을 깨지 않도록 격리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  @Value("${ddasoom.service-url}")
  private String serviceUrl;   // 재설정 링크의 프론트 베이스 URL — 환경변수 주입(A-0)

  private final RedisTemplate<String, String> redisTemplate;
  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final MailUtil mailUtil;
  private final RedisTokenService redisTokenService;

  private final SecureRandom random = new SecureRandom();   // 인증코드 생성 — 예측 불가능해야 하므로 SecureRandom

  // ── TTL·제한 상수: 전부 메일/에러 문구의 숫자와 일치시킨다 (문구와 코드가 어긋나면 사용자 혼란) ──
  private static final Duration AUTH_CODE_TTL = Duration.ofMinutes(3);   // 메일 문구 "3분"과 일치
  private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);   // 인증 후 가입까지 유예

  private static final int MAX_AUTH_CODE_ATTEMPTS = 5;   // 초과 시 코드 폐기, 재발송 요구 (코드 브루트포스 방지)
  private String authCodeAttemptKey(String email) { return "authCodeAttempt:" + email; }

  private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);  // 메일 안내 문구와 일치시킬 것
  private String cooldownKey(String email) { return "authCodeCooldown:" + email; }

  // IP 단위 발송 제한 — 이메일 단위 쿨다운(60초)만으로는 "이메일을 바꿔가며" 대량 발송하는
  // 악용을 못 막음 (피드백 1-5). 시간당 10회는 공용 IP(학원/카페) 오탐을 완화한 절충값.
  private static final int MAX_AUTH_MAILS_PER_IP = 10;
  private static final Duration IP_LIMIT_WINDOW = Duration.ofHours(1);   // 에러 문구 "1시간"과 일치
  private String ipLimitKey(String clientIp) { return "authCodeSendIp:" + clientIp; }

  // 키 설계 (컨벤션: {용도}:{식별자})
  private String authCodeKey(String email) { return "authCode:" + email; }
  private String verifiedKey(String email) { return "verified:" + email; }

  /**-------------------------------------------------------------------------------------------------
   * 회원가입 요청 중 , 이메일 인증 로직
   * -------------------------------------------------------------------------------------------------*/

  /**
   * 인증 코드 발송 (재발송 겸용). clientIp는 컨트롤러가 추출해 전달 (서비스의 HttpServletRequest 의존 금지).
   *
   * <p>검증 순서가 곧 방어선의 우선순위다: IP 제한(가장 넓은 악용) → 이메일 쿨다운 → 이미 가입 여부.
   * 발송에 실패하면 쿨다운·IP카운트를 기록하지 않아, 실패한 시도가 정상 사용자의 재시도를 막지 않게 한다.
   */
  public void sendAuthCode(String email, String clientIp) {
    // IP 단위 제한을 이메일 쿨다운보다 먼저 — 더 넓은 악용 방어선이 최전방
    String sendCountValue = redisTemplate.opsForValue().get(ipLimitKey(clientIp));
    if (sendCountValue != null && Integer.parseInt(sendCountValue) >= MAX_AUTH_MAILS_PER_IP) {
      throw new AuthException(AuthErrorCode.AUTH_CODE_IP_LIMIT);
    }

    if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey(email)))) {
      throw new AuthException(AuthErrorCode.AUTH_CODE_COOLDOWN);
    }
    if (memberRepository.existsByEmail(email)) {
      throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
    }

    // 재발송은 "이전 인증 상태를 백지화"한다 — 옛 verified/시도카운트가 남아 새 코드와 엉키지 않게
    redisTemplate.delete(verifiedKey(email));
    redisTemplate.delete(authCodeAttemptKey(email));   // A-6 연동 — 재발송은 새 시도 기회

    String code = String.format("%06d", random.nextInt(1_000_000));
    redisTemplate.opsForValue().set(authCodeKey(email), code, AUTH_CODE_TTL);

    mailUtil.sendAuthCodeEmail(email, code);   // 발송 실패 시 여기서 throw — 쿨다운/IP카운트는 미기록
    redisTemplate.opsForValue().set(cooldownKey(email), "sent", RESEND_COOLDOWN);  // 발송 성공 후에만

    // 실제 발송 성공 건만 카운트 (실패/거절 요청은 미포함 — 쿨다운과 동일 원칙).
    // INCR 후 첫 증가에만 TTL 부여 — 기존 authCodeAttempt 카운터와 같은 패턴
    // (SET+EXPIRE 2연산 대신 INCR로 원자적 증가 → 동시 요청에도 카운트 유실 없음)
    Long sendCount = redisTemplate.opsForValue().increment(ipLimitKey(clientIp));
    if (sendCount != null && sendCount == 1) {
      redisTemplate.expire(ipLimitKey(clientIp), IP_LIMIT_WINDOW);
    }
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
      // 5회 초과 → 브루트포스 간주, 코드 자체를 폐기 (재발송부터 다시).
      // 6자리(100만 조합)라 무제한 시도를 허용하면 뚫릴 수 있어 시도 횟수로 상한을 건다.
      if (attempts != null && attempts >= MAX_AUTH_CODE_ATTEMPTS) {
        redisTemplate.delete(authCodeKey(email));
      }
      // 불일치와 만료를 같은 코드로 응답 — "코드는 맞는데 만료" 같은 정보도 공격 힌트라 노출 안 함
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
    // 1. 이메일 인증 완료 여부 확인 (30분 초과 시 verified가 만료돼 재인증 필요)
    if (redisTemplate.opsForValue().get(verifiedKey(request.getEmail())) == null) {
        throw new AuthException(AuthErrorCode.EMAIL_NOT_VERIFIED);
    }

    // 2. 중복 재확인 — 인증~가입 사이 텀에 같은 이메일/닉네임이 선점됐을 수 있어 최종 저장 직전 다시 확인.
    //    (DB 유니크 제약이 최종 방어선이지만, 여기서 걸러 사용자에게 명확한 에러코드를 준다)
    if (memberRepository.existsByEmail(request.getEmail())) {
        throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
    }
    if (memberRepository.existsByNickname(request.getNickname())) {
        throw new AuthException(AuthErrorCode.NICKNAME_ALREADY_EXISTS);
    }

    // 3. 저장 (비밀번호 인코딩은 toEntity에 위임 — 평문이 서비스 로직을 흐르지 않게)
    Member member = memberRepository.save(request.toEntity(passwordEncoder));

    // 4. 인증 플래그 소진 — 한 번 가입에 쓴 verified는 재사용 못 하도록 삭제
    redisTemplate.delete(verifiedKey(request.getEmail()));

    // 5. 환영 메일 — 실패해도 가입은 성공해야 하므로 로그만 남김 (외부 I/O 격리, 컨벤션 6)
    try {
        mailUtil.sendWelcomeEmail(member.getEmail());
    } catch (AuthException e) {
        log.warn("환영 메일 발송 실패 (가입은 정상 처리) - email: {}", member.getEmail());
    }

    return SignupResponse.from(member);
  }

    /**-------------------------------------------------------------------------------------------------
   *  비밀번호 재설정 메일 발송.
   *  대상이 없어도(미가입·탈퇴·소셜전용) 예외 없이 조용히 종료 → 컨트롤러는 항상 동일 성공 응답.
   *  "메일이 갔는지"로 이메일 가입 여부를 추정하는 열거 공격을 막기 위한 설계.
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
        if (memberId == null) {   // 토큰 만료/위조/이미 사용됨
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
        redisTokenService.deleteRefreshTokens(memberId);    // 자격증명 교체 → 기존 세션 무효화(재로그인 필요)
    }

    /** 닉네임 사용 가능 여부 — 사용 가능하면 true. 형식 검증은 최종 제출의 @Pattern이 담당(여기선 중복만) */
    public boolean isNicknameAvailable(String nickname) {
        return !memberRepository.existsByNickname(nickname);
    }

}