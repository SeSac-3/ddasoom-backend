package com.paw.ddasoom.member.dto.request;

import com.paw.ddasoom.member.domain.MemberStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 관리자 회원 상태 변경 요청 — 정의 밖 문자열은 파싱 핸들러가 400 INVALID_INPUT으로 처리 (A-3 연계) */
@Getter
@NoArgsConstructor
public class MemberStatusUpdateRequest {

  @NotNull(message = "변경할 상태(ACTIVE/HIDDEN)를 입력해 주세요.")
    private MemberStatus status;
}
