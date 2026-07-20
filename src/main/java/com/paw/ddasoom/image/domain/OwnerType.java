package com.paw.ddasoom.image.domain;

/**
 * 이미지 소유자 타입 — 버킷/URL 분기의 유일한 기준점
 *
 * <p>공개 여부에 따라 저장 버킷과 URL 방식이 갈린다:
 * <ul>
 *   <li>isPublic = true  → ddasoom-public, 영구 정적 URL</li>
 *   <li>isPublic = false → ddasoom-private, Presigned URL(30분)</li>
 * </ul>
 *
 * <p>⚠️ 공개 여부 분기는 반드시 {@link #isPublic()}으로만 —
 * 사용처에서 {@code ownerType == QNA} 같은 직접 비교가 보이면 기준점이 새는 것.
 * 새 타입 추가 시 상수 한 줄 + isPublic 값만 결정하면 나머지 분기는 자동으로 따라온다.
 */
public enum OwnerType {
    POST(true), NOTICE(true), ANIMAL(true), FAQ(true), QNA(false), QNA_COMMENT(false);;

    private final boolean isPublic;

    OwnerType(boolean isPublic) { this.isPublic = isPublic; }

    public boolean isPublic() { return this.isPublic; }
}
