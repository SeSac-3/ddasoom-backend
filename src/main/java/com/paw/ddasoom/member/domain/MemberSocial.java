package com.paw.ddasoom.member.domain;

import com.paw.ddasoom.common.util.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_social", uniqueConstraints = {
    @UniqueConstraint(name = "uk_member_socials_provider", columnNames = {"provider", "provider_id"}) // 복합 유니크 키
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSocial extends BaseTimeEntity{

  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "member_social_id")
  private Long id;

  @Column(nullable = false, length = 20)
  private String provider; // KAKAO, NAVER, GOOGLE

  @Column(name = "provider_id", nullable = false)
  private String providerId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false,
     foreignKey = @ForeignKey(name = "fk_member_socials_member"))
  private Member member;

  @Builder
  public MemberSocial(String provider, String providerId, Member member) {
      this.provider = provider;
      this.providerId = providerId;
      this.member = member;
  }
}
