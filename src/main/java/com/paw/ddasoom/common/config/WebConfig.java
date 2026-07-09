package com.paw.ddasoom.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.paw.ddasoom.common.security.CorsProperties;

@Configuration
public class WebConfig implements WebMvcConfigurer{

  private final CorsProperties corsProperties;

  public WebConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    /**  cors 설정 추후 수정 필요
    * @Author : 지훈
    */

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 쉼표로 구분된 문자열을 배열로 만든다.
                .allowedOrigins(corsProperties.getAllowedOrigins().split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE","OPTIONS")
                .allowedHeaders("*")
                // 쿠키나 인증 헤더와 같은 자격 증명(Credentials)을 허용할지 여부.
                // 프론트엔드와 세션/토큰을 주고받으려면 true로 설정해야 합니다.
                .allowCredentials(true)
                // 브라우저에서 preflight 요청 결과를 캐시할 시간을 초 단위로 지정합니다.
                .maxAge(3600);
    }

    @Bean
    public RestClient restClient(){
        return RestClient.builder()
            .baseUrl("${api.base-url}")
            .build();
    }



}
