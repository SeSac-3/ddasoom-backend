package com.paw.ddasoom.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger(OpenAPI 3) 문서 설정.
 * - JWT Bearer 인증 스킴을 전역 등록 → Swagger UI에서 "Authorize" 버튼으로 AT를 넣고 보호 API 테스트 가능.
 * - AT는 로그인 응답 body의 accessToken을 사용 (RT는 HttpOnly 쿠키라 Swagger에서 다루지 않음).
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI ddasoomOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("따숨(Ddasoom) API")
                        .description("유기동물 임시보호 플랫폼 API 문서. "
                                + "Authorize 버튼에 로그인으로 발급받은 Access Token을 입력하면 보호 API를 테스트할 수 있습니다.")
                        .version("v1"))
                // 모든 API에 기본 인증 요구 표시 (공개 API는 각 컨트롤러에서 문서상 예외 안내)
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}