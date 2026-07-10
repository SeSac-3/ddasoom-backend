package com.paw.ddasoom.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paw.ddasoom.member.domain.MemberSocial;
import com.paw.ddasoom.member.domain.SocialProvider;

public interface MemberSocialRepository extends JpaRepository<MemberSocial, Long>{
  Optional<MemberSocial> findByProviderAndProviderId(SocialProvider provider, String providerId);

  /** 관리자 회원 상세 — 연동된 소셜 목록 */
  List<MemberSocial> findByMemberId(Long memberId);
}
