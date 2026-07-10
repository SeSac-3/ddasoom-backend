package com.paw.ddasoom.support.domain;

import java.time.LocalDateTime;

import com.paw.ddasoom.common.util.BaseTimeEntity;
import com.paw.ddasoom.member.domain.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notice")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/*
 * [무분별한 객체 생성 차단]
 * JPA 기본 생성자는 필수이되 외부에서 'new Notice()'로 빈 객체를 만드는 것은 막아,
 * 불완전한 상태의 객체가 돌아다니는 것을 원천 차단 (PROTECTED 레벨)
 */
public class Notice extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "notice_id")
  private Long id;

  /*
   * [성능 최적화 (지연 로딩)]
   * 공지사항을 조회할 때 작성자(Member) 정보까지 한 번에 가져오지 않고,
   * 실제 작성자 데이터가 필요한 시점에만 DB를 조회하도록 설정
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member; // 작성자(관리자)

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false,columnDefinition = "TEXT")
  private String content;

  /*
   * [단일 진실 공급원(SSOT) 규칙 적용]
   * 공지사항의 기본 노출 상태(true)를 필드 선언부에 명시함으로써,
   * 생성 방식이나 DB 조회 여부와 무관하게 이 필드의 '기본 시작점' 명확히 선언
   */
  @Column(nullable = false)
  private Boolean isVisible = true;

    @Column(name = "pin_order")
    private Integer pinOrder; // NULL = 미고정

  /*
   * [Soft Delete(논리 삭제)를 위한 데이터 보존]
   * DB에서 데이터를 아예 완전히 지워버리는(Hard Delete) 대신,
   * 삭제된 시각을 기록하여 데이터 복구 및 이력 관리가 가능하도록 설정
   */
  @Column(columnDefinition = "DATETIME(6)")
  private LocalDateTime deletedAt;

  /*
   * [안전하고 제한된 생성자 제공]
   * 외부에서 공지사항을 처음 '생성'할 때 채워야 하는 필수 필드만 빌더로 노출
   * 자동 생성되는 ID, 시스템이 관리할 삭제일(deletedAt) 등 빌더 제외 - 안정성을 높임
   */
  @Builder
  public Notice(Member member, String title, String content) {
    this.member = member;
    this.title = title;
    this.content = content;
  }

  // =========================================================================
  // 비즈니스 메서드 (의도가 명확한 상태 변경 행위들)
  // 무분별한 Setter를 배제하고, 객체 스스로가 자신의 상태를 변경하도록 도메인 로직 응집
  // =========================================================================

  /* 공지사항 내용 수정 */
  public void update(String title, String content) {
    this.title = title;
    this.content = content;
  }

  /* 공지사항 노출 여부 전환 */
  public void changeVisibility(boolean isVisible) {
    this.isVisible = isVisible;
  }

  /* 리치도메인 메서드 -> 고정 (순서 지정) */
  public void pin(int order) {
      this.pinOrder = order;
  }

  /* 리치도메인 메서드 -> 고정 해제 */
  public void unpin() {
      this.pinOrder = null;
  }

  /* */
  public boolean isPinned() {
    return this.pinOrder != null;
  }

  /* 논리 삭제 수행 */
  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }

  /* 현재 상태(삭제여부) 조회 */
  public boolean isDeleted() {
    return this.deletedAt != null;
  }
}
