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
        .csrf(csrf -> csrf.disable())
        //경로별 인가 작업
                .authorizeHttpRequests(authorize -> authorize
                        // ⚠️ requestMatchers는 선언 순서대로 매칭 — 구체적 경로(예외)를 넓은 경로보다 먼저!

                        // 1. 공개 경로의 "예외" — auth 하위지만 토큰 필수
                        .requestMatchers("/api/auth/logout").authenticated()

                        // 2. GUEST 전용 — members 규칙보다 먼저
                        .requestMatchers("/api/members/me/signup-complete").hasRole("GUEST")

                        // 3. 공개 경로 (SecurityConstants에서 관리)
                        .requestMatchers(SecurityConstants.PUBLIC_URIS).permitAll()

                        // 4. 회원 리소스 — USER/ADMIN
                        .requestMatchers("/api/members/**").hasAnyRole("USER", "ADMIN")

                        // 5. 관리자 전용
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 6. 그 외 전부 인증 필요 (미분류 = 잠금이 기본값)
                        .anyRequest().authenticated()
                      )
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
                                .userService(customOAuth2UserService))          // 회원 판별/생성 (5번)
                        .successHandler(oAuth2SuccessHandler)                   // RT 쿠키 + 프론트 리다이렉트 (7번)
                        .failureHandler(oAuth2FailureHandler))                  // 에러코드 쿼리 전달 (8번)

                // JWT 인증 필터 등록 (UsernamePasswordAuthenticationFilter 앞)
                .addFilterBefore(authJwtTokenFilter, UsernamePasswordAuthenticationFilter.class)

                // 인증/인가 실패 응답 통일 — 필터와 반드시 세트 (없으면 기본 HTML/리다이렉트)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)   // 401
                        .accessDeniedHandler(customAccessDeniedHandler))            // 403
                

                //세션 설정
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
  }
  

      // 보안 필터에서 시큐리티에서 CORS 설정 처리
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(corsProperties.getAllowedOrigins().split(",")));
        configuration.setAllowedMethods(Collections.singletonList("*"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        configuration.setMaxAge(3600L);
        configuration.setExposedHeaders(Collections.singletonList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 URL에 적용
        return source;
    }
}
