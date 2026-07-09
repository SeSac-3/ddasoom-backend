package com.paw.ddasoom.member.domain;

import java.util.Locale;

/** 소셜 로그인 제공자 — yml registrationId(kakao/naver/google)와 매핑 */
public enum SocialProvider {

  KAKAO, NAVER, GOOGLE;

  /** Spring Security의 registrationId(소문자)를 enum으로 변환 */
  public static SocialProvider from(String registrationId) {
      return SocialProvider.valueOf(registrationId.toUpperCase(Locale.ROOT));
  }

}
