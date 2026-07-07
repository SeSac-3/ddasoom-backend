package com.paw.ddasoom.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.paw.ddasoom.auth.exception.AuthErrorCode;
import com.paw.ddasoom.auth.exception.AuthException;
import com.paw.ddasoom.common.security.CustomUserDetails;

public class SecurityUtil {
private SecurityUtil() {} // 유틸 클래스 — 인스턴스화 방지

/** 현재 로그인한 회원의 PK 반환. 미인증 컨텍스트에서 호출 시 AUTH_102 */
public static Long getMemberId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
        throw new AuthException(AuthErrorCode.UNAUTHORIZED);
    }
    return userDetails.getMemberId();
}

}
