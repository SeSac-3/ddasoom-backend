package com.paw.ddasoom.auth.oauth2;

import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.auth.exception.AuthErrorCode;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.domain.MemberSocial;
import com.paw.ddasoom.member.domain.MemberStatus;
import com.paw.ddasoom.member.domain.Role;
import com.paw.ddasoom.member.repository.MemberRepository;
import com.paw.ddasoom.member.repository.MemberSocialRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 소셜 로그인 사용자 판별/생성 (SECURITY-FLOW 소셜 흐름의 본체)
 *   provider+providerId 조회 → 기존 연동 회원이면 그대로 로그인
 *   없으면 이메일 충돌 검사 → 충돌 시 AUTH_106 (수동 연동 정책)
 *   신규면 Member(GUEST, password=null, nickname=null) + MemberSocial 생성
 *
 * <p>여기서 던진 OAuth2AuthenticationException은 OAuth2FailureHandler가 받아 프론트로 전달한다.
 * 이 서비스는 컨트롤러가 아니라 Security 필터 체인 내부에서 호출되므로, AuthException을 던지면
 * GlobalExceptionHandler에 닿지 못하고 500이 뜬다 — 그래서 반드시 OAuth2AuthenticationException으로
 * 감싸고, 에러코드 문자열만 실어 보낸다(FailureHandler가 프론트 콜백 URL의 error 쿼리로 변환).
 *
 * <p>핵심 정책: 소셜 신원은 이메일이 아니라 <b>provider+providerId</b>다. 이메일은 위·변조 가능성이
 * 있어(특히 네이버의 연락처 이메일), 이메일만으로 기존 계정에 자동 연동하면 계정 탈취로 이어질 수 있다.
 * 그래서 이메일 충돌 시 자동 연동하지 않고 AUTH_106으로 막는다(수동 연동 정책).
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService{

public static final String MEMBER_ID_KEY = "memberId";   // SuccessHandler가 꺼내 쓸 키

  private final MemberRepository memberRepository;
  private final MemberSocialRepository memberSocialRepository;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) {
      // 1. 부모가 provider의 user-info-uri를 호출해 원본 attributes 획득
      OAuth2User oAuth2User = super.loadUser(userRequest);
      String registrationId = userRequest.getClientRegistration().getRegistrationId();

      // 2. 3사 구조 차이 흡수 — 카카오/네이버/구글의 응답 JSON 구조가 제각각이라 OAuth2UserInfo로 통일
      OAuth2UserInfo userInfo = OAuth2UserInfo.of(registrationId, oAuth2User.getAttributes());

      // 3. 이메일 미제공 방어 (카카오 동의 거부 / 네이버 연락처 이메일 미등록)
      //    이메일이 없으면 회원 식별·충돌 검사가 불가능하므로 여기서 즉시 중단
      if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
          throw new OAuth2AuthenticationException(
                  new OAuth2Error(AuthErrorCode.SOCIAL_EMAIL_REQUIRED.getCode()));
      }

      // 4. 회원 판별/생성
      Member member = findOrCreateMember(userInfo);

      // 5. SuccessHandler에 memberId만 전달하면 충분 — 인가 규칙은 우리 JWT 필터가 담당하므로
      //    여기서 부여하는 권한(ROLE_xxx)은 Spring Security 계약상 필요한 형식상 최소값일 뿐,
      //    실제 인가에는 쓰이지 않는다(우리 AT의 role claim이 진짜 권한 출처).
      return new DefaultOAuth2User(
              java.util.List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name())),
              Map.of(MEMBER_ID_KEY, member.getId()),
              MEMBER_ID_KEY);
  }

  private Member findOrCreateMember(OAuth2UserInfo userInfo) {
      // 기존 연동 회원 — 신원은 이메일이 아니라 provider+providerId (위 클래스 주석의 정책)
      return memberSocialRepository
              .findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())
              .map(MemberSocial::getMember)
              .map(this::validateLoginable)
              .orElseGet(() -> signupSocialMember(userInfo));
  }

  /** 신규 소셜 가입 — 이메일 충돌 시 수동 연동 정책(AUTH_106) */
  private Member signupSocialMember(OAuth2UserInfo userInfo) {
      // 이메일이 이미 존재 = 일반 가입 or 다른 소셜로 가입된 계정 → 자동 연동 금지(계정 탈취 방지)
      if (memberRepository.existsByEmail(userInfo.getEmail())) {
          throw new OAuth2AuthenticationException(
                  new OAuth2Error(AuthErrorCode.SOCIAL_EMAIL_ALREADY_EXISTS.getCode()));
      }

      // 닉네임/이름/전화번호는 추가정보 입력(USER 승급) 시에만 채운다 — 소셜 닉네임 미사용 (팀 결정).
      // 소셜이 주는 닉네임을 쓰면 유니크 충돌·개인정보 동기화 문제가 생겨, 우리 입력값만 신뢰한다.
      Member member = memberRepository.save(Member.builder()
              .email(userInfo.getEmail())
              .password(null)                 // 소셜 전용 회원 표식 — 비밀번호 로그인 차단 근거
              .role(Role.GUEST)               // 추가정보 입력 전까지 GUEST — USER API 접근 불가
              .build());

      memberSocialRepository.save(MemberSocial.builder()
              .provider(userInfo.getProvider())
              .providerId(userInfo.getProviderId())
              .member(member)
              .build());

      log.debug("소셜 신규 가입(GUEST) - provider: {}", userInfo.getProvider());
      return member;
  }

  /** 탈퇴·제재 회원의 소셜 로그인 차단 — 일반 로그인(LoginService)과 동일 방어.
   *  소셜은 provider 본인 인증을 통과한 뒤라 사유를 안내해도 안전 → 전용 코드로 구분 노출.
   *  (AUTH_109가 "provider 본인 인증 통과자에게만 보이는 응답이라 노출해도 안전"하다는 기존 논리를 AUTH_110에도 동일 적용) */
  private Member validateLoginable(Member member) {
      if (member.isDeleted()) {
          throw new OAuth2AuthenticationException(
                  new OAuth2Error(AuthErrorCode.WITHDRAWN_MEMBER.getCode()));   // AUTH_109
      }
      if (member.getStatus() == MemberStatus.HIDDEN) {
          throw new OAuth2AuthenticationException(
                  new OAuth2Error(AuthErrorCode.BLOCKED_MEMBER.getCode()));     // AUTH_110
      }
      return member;
  }

}
