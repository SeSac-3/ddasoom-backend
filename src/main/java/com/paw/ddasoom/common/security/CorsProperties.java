package com.paw.ddasoom.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")  // "app.cors"로 시작하는 프로퍼티 바인딩
public class CorsProperties {
  private String allowedOrigins;
}
