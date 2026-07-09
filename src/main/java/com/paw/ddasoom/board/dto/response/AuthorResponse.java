package com.paw.ddasoom.board.dto.response;

import com.paw.ddasoom.member.domain.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
public class AuthorResponse {

    private final Long memberId;
    private final String nickname;

    @Builder
    private AuthorResponse(Long memberId, String nickname) {
        this.memberId = memberId;
        this.nickname = nickname;
    }

    public static AuthorResponse from(Member member) {
        return AuthorResponse.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .build();
    }
}