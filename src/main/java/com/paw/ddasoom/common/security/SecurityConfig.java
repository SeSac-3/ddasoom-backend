package com.paw.ddasoom.common.security;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.paw.ddasoom.auth.oauth2.CustomOAuth2UserService;
import com.paw.ddasoom.auth.oauth2.OAuth2FailureHandler;
import com.paw.ddasoom.auth.oauth2.OAuth2SuccessHandler;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security 설정 — 인가 규칙·JWT 필터·OAuth2·CORS·세션 정책의 단일 조립 지점.
 *
 * <p>설계 핵심:
 * ① 기본 잠금(anyRequest authenticated) — 등록 안 된 API는 자동으로 인증 필수. 등록 누락의 결과가
 *    "개방 사고"가 아니라 "401 문의"가 되도록, 안전한 방향으로 실패하게 한다.
 * ② STATELESS — 서버 세션을 만들지 않는다(JWT 기반). CSRF도 헤더 토큰 방식이라 비활성.
 * ③ CORS는 여기 한 곳에서만 — WebConfig의 CORS 설정은 제거해 이중 관리로 인한 혼선을 없앴다(A-3).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CorsProperties corsProperties;
  private final AuthJwtTokenFilter authJwtTokenFilter;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;
  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2SuccessHandler oAuth2SuccessHandler;
  private final OAuth2FailureHandler oAuth2FailureHandler;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        //CORS 설정 - 보안 필터에서 허용
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        // CSRF 비활성: 인증을 Authorization 헤더(Bearer)로 하므로 쿠키 자동전송 기반 CSRF가 성립 안 함.
        // (RT는 쿠키지만 SameSite=Lax + reissue/logout 전용 Path라 CSRF 표면이 최소)
        .csrf(csrf -> csrf.disable())
        //경로별 인가 작업
                .authorizeHttpRequests(authorize -> authorize
                        // ⚠️ requestMatchers는 선언 순서대로 매칭 — 구체적 경로(예외)를 넓은 경로보다 먼저!
                        // 순서를 바꾸면 규칙이 "조용히" 죽는다(에러 없이 잘못 매칭되므로 발견이 늦다).
                        // (기존 "/api/auth/logout → authenticated" 예외 규칙은 삭제됨:
                        //  auth 낱개 등록으로 logout이 어디에도 안 걸려 6번 anyRequest에서 자연 잠금 — 동일 동작)

                        // 1. GUEST 전용 — members 규칙보다 먼저 (뒤에 두면 3번 members에 먼저 걸려 죽음)
                        .requestMatchers("/api/members/me/signup-complete").hasRole("GUEST")

                        // 2. 공개 경로 (SecurityConstants에서 관리) — auth/oauth2/swagger 등
                        .requestMatchers(SecurityConstants.PUBLIC_URIS).permitAll()

                        // 3. 회원 리소스 — USER/ADMIN
                        .requestMatchers("/api/members/**").hasAnyRole("USER", "ADMIN")

                        // 4. USER 전용 등록 경로 — GUEST 차단
                        //    (⚠️ 반드시 1·2번보다 뒤 — 앞에 두면 GUEST 승급/공개 경로가 죽는다)
                        .requestMatchers(SecurityConstants.USER_URIS).hasAnyRole("USER", "ADMIN")

                        // 5. 관리자 전용 — 경로를 /api/admin 하위로 잡기만 하면 자동 잠금
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 6. 그 외 전부 인증 필요 (미분류 = 잠금이 기본값)
                        //    단 authenticated는 로그인 여부만 검사 — GUEST 차단 필요 시 USER_URIS에 등록
                        .anyRequest().authenticated()
                      )
                // 우리는 JWT 기반이라 폼로그인·httpBasic·시큐리티 기본 로그아웃을 전부 끈다(자체 구현으로 대체)
                .formLogin(auth -> auth.disable())
                .httpBasic(auth -> auth.disable())
                .logout(logout -> logout.disable())
                // OAuth2 소셜 로그인 (카카오/네이버/구글)
                .oauth2Login(oauth2 -> oauth2
                        //두 경로를 /api 하위로 커스텀 — 프론트 프록시(/api)를 타기 위함
                        .authorizationEndpoint(auth -> auth
                                .baseUri("/api/oauth2/authorization"))          // 인가 시작: 소셜 버튼이 가리키는 곳
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/api/login/oauth2/code/*"))           // 콜백: 3사 콘솔에 등록한 그 경로
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))          // 회원 판별/생성
                        .successHandler(oAuth2SuccessHandler)                   // RT 쿠키 + 프론트 리다이렉트
                        .failureHandler(oAuth2FailureHandler))                  // 에러코드 쿼리 전달

                // JWT 인증 필터 등록 — UsernamePasswordAuthenticationFilter '앞'에 둬서
                // 폼 인증이 돌기 전에 우리 토큰 검증이 SecurityContext를 채우도록 한다.
                .addFilterBefore(authJwtTokenFilter, UsernamePasswordAuthenticationFilter.class)

                // 인증/인가 실패 응답 통일 — 필터와 반드시 세트 (없으면 기본 HTML/리다이렉트가 나가 API 규격이 깨짐).
                // 401은 EntryPoint, 403은 AccessDeniedHandler가 ApiResponse 포맷으로 응답.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)   // 401
                        .accessDeniedHandler(customAccessDeniedHandler))            // 403
                

                // 세션 미사용 — JWT라 서버가 세션 상태를 들지 않는다(수평 확장·무상태의 근거)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
  }
  

      // 보안 필터에서 시큐리티에서 CORS 설정 처리
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용 오리진은 환경변수(콤마 구분)에서 주입 — 배포 환경마다 다른 프론트 주소를 코드 수정 없이 관리
        configuration.setAllowedOrigins(Arrays.asList(corsProperties.getAllowedOrigins().split(",")));
        configuration.setAllowedMethods(Collections.singletonList("*"));
        // 쿠키(RT) 전송을 허용해야 하므로 allowCredentials=true. 이 경우 allowedOrigins에 "*"는 쓸 수 없어
        // 반드시 구체적 오리진을 지정해야 한다(브라우저 정책).
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        configuration.setMaxAge(3600L);
        // 프론트가 응답 헤더의 Authorization(재발급된 AT 등)을 읽을 수 있게 노출
        configuration.setExposedHeaders(Collections.singletonList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 URL에 적용
        return source;
    }
}
