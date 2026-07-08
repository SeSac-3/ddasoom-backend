package com.paw.ddasoom.auth.oauth2;

import java.util.Map;

import com.paw.ddasoom.member.domain.SocialProvider;

/**
 * 3사 사용자 정보 응답의 구조 차이를 흡수하는 파서.
 *   구글:   { sub, email, name }                                      — 평평한 구조
 *   카카오: { id, kakao_account: { email, profile: { nickname } } }    — 이중 중첩
 *   네이버: { response: { id, email, name } }                          — response 래핑
 * 사용처(CustomOAuth2UserService)는 provider별 구조를 몰라도 됨.
 */
public class OAuth2UserInfo {

  private final SocialProvider provider;
  private final String providerId;
  private final String email;    // null 가능 — 동의 거부/미등록 (호출자가 검증)
  private final String name;     // null 가능

  private OAuth2UserInfo(SocialProvider provider, String providerId, String email, String name) {
    this.provider = provider;
    this.providerId = providerId;
    this.email = email;
    this.name = name;
  }

  public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
      SocialProvider provider = SocialProvider.from(registrationId);
      return switch (provider) {
          case GOOGLE -> ofGoogle(attributes);
          case KAKAO -> ofKakao(attributes);
          case NAVER -> ofNaver(attributes);
      };
  }

  private static OAuth2UserInfo ofGoogle(Map<String, Object> attributes) {
      return new OAuth2UserInfo(
              SocialProvider.GOOGLE,
              String.valueOf(attributes.get("sub")),
              (String) attributes.get("email"),
              (String) attributes.get("name"));
  }

  @SuppressWarnings("unchecked")
  private static OAuth2UserInfo ofKakao(Map<String, Object> attributes) {
      Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());
      Map<String, Object> profile = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Map.of());

      return new OAuth2UserInfo(
              SocialProvider.KAKAO,
              String.valueOf(attributes.get("id")),        // 카카오 고유번호는 Long으로 옴 → 문자열화
              (String) kakaoAccount.get("email"),
              (String) profile.get("nickname"));
  }

  @SuppressWarnings("unchecked")
  private static OAuth2UserInfo ofNaver(Map<String, Object> attributes) {
      // user-name-attribute: response — 실제 정보는 response 객체 안에 있음
      Map<String, Object> response = (Map<String, Object>) attributes.getOrDefault("response", Map.of());

      return new OAuth2UserInfo(
              SocialProvider.NAVER,
              (String) response.get("id"),
              (String) response.get("email"),              // ⚠️ 연락처 이메일 — 비검증 가능 (수동 연동 정책의 근거)
              (String) response.get("name"));
  }

  public SocialProvider getProvider() { return provider; }
  public String getProviderId() { return providerId; }
  public String getEmail() { return email; }
  public String getName() { return name; }
}
