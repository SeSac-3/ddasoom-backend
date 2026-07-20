package com.paw.ddasoom.member.domain;

import java.time.LocalDateTime;

import com.paw.ddasoom.common.util.BaseTimeEntity;
import com.paw.ddasoom.member.exception.MemberErrorCode;
import com.paw.ddasoom.member.exception.MemberException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "member" , uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_member_nickname", columnNames = "nickname")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity{

  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "member_id")
  private Long id;

  @Column(nullable = false)
  private String email;

  private String password; // 소셜 유저는 null

  @Column(length = 50)
  private String name;

  @Column(length = 20)
  private String nickname;

  @Column(length = 20)
  private String tel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EventStatus eventStatus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MemberStatus status;

  @Column(columnDefinition = "DATETIME(6)")
  private LocalDateTime deletedAt;

  @Builder
  public Member(String email, String password, String name, String nickname, String tel, Role role) {
      this.email = email;
      this.password = password;
      this.name = name;
      this.nickname = nickname;
      this.tel = tel;
      this.role = role != null ? role : Role.GUEST;
      this.eventStatus = EventStatus.NONE;   // 신규 회원 기본 상태
      this.status = MemberStatus.ACTIVE;     // 신규 회원 = 정상 노출 상태
  }

  // 리치도메인 메서드 -> SNS 회원가입 추가 정보 수용 + Update
  public void updateExtraInfo(String name, String nickname, String tel) {
      this.name = name;
      this.nickname = nickname;
      this.tel = tel;
      this.role = Role.USER; // 추가 정보 입력 시 USER로 권한 승급
  }

  // 리치도메인 메서드 -> Soft delete 
  public void softDelete() {
    // 삭제 여부 판단 기준을 deletedAt이 null인지 아닌지로 변경
    if (this.deletedAt != null) {
        throw new MemberException(MemberErrorCode.ALREADY_DELETED_MEMBER);
    }
    this.deletedAt = LocalDateTime.now();
  }

  // 4. 편의 메서드 (기존 서비스 로직 호환용)
  public boolean isDeleted() {
      return this.deletedAt != null;
  }

  // 리치도메인 메서드 -> 프로필(닉네임/전화번호) 수정
  public void updateProfile(String name, String nickname, String tel) {
        this.name = name;
        this.nickname = nickname;
        this.tel = tel;
  }

  // 리치도메인 메서드 -> 비밀번호 변경 (인코딩은 서비스 책임 — 인코딩된 값만 받는다)
  public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
  }

  // 리치도메인 메서드 -> 계정 복구 (관리자 전용 — soft delete 역연산)
  public void restore() {
    if (this.deletedAt == null) {
        throw new MemberException(MemberErrorCode.NOT_DELETED_MEMBER);
    }
    this.deletedAt = null;
  }

  // 리치도메인 메서드 -> 이벤트 참여 상태 변경 (board 도메인이 게시글 작성 수 기준으로 호출)
  public void updateEventStatus(EventStatus eventStatus) {
      this.eventStatus = eventStatus;
  }

  /**
   * 리치도메인 메서드
   * 노출 상태 전환 (ACTIVE ↔ HIDDEN) — 관리자 수동 조작 전용.
   * 탈퇴 회원 차단은 호출부의 getMember(활성 조회)가 담당하므로 여기선 검사하지 않는다.
   */
  public void changeStatus(MemberStatus status) {
      this.status = status;
  }

}
