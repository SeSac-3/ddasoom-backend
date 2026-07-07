package com.paw.ddasoom.foster.domain;

public enum FosterStatus {
  PENDING, // 신청 대기
  REJECTED, // 신청 거절
  FOSTERING, // 임시보호 진행 중
  EXTENDED, // 임시보호 연장
  ENDED // 임시보호 종료
}
