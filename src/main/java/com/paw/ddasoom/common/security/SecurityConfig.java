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
                        .requestMatchers("/api/members/me/signup-complete").hasRole("GUEST")
                        // 🔽 여기에 로그인 없이 접근을 허용할 URL 경로 목록을 작성합니다.
                        .requestMatchers(
                                SecurityConstants.PUBLIC_URIS
                        ).permitAll() // 위에 명시된 경로들은 모두 허용

                        // 여기에 관리자만 접근을 허용할 URL 경로 목록 작성
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")  // 관리자(ADMIN)

                        // 🔽 위에서 허용한 경로 외의 나머지 모든 요청은 반드시 인증(로그인)을 거쳐야 합니다.
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
