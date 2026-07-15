package com.paw.ddasoom.board.dto.response;

import com.paw.ddasoom.member.domain.Member;
import lombok.Builder;
import lombok.Getter;

/**
 * 작성자 정보 서브 DTO — Post/PostComment 응답에서 재사용.
 */
@Getter
public class AuthorResponse {

    private final Long memberId;
    private final String nickname;

    @Builder
    private AuthorResponse(Long memberId, String nickname) {
        this.memberId = memberId;
        this.nickname = nickname;
    }

    /** 상세 경로용 — 엔티티에서 직접 변환 */
    public static AuthorResponse from(Member member) {
        return AuthorResponse.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .build();
    }

    /** 목록(projection) 경로용 — Member 엔티티 없이 평면 값으로 생성 */
    public static AuthorResponse from(Long memberId, String nickname) {
        return AuthorResponse.builder()
                .memberId(memberId)
                .nickname(nickname)
                .build();
    }
}