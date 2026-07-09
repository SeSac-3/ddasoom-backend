package com.paw.ddasoom.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paw.ddasoom.member.domain.MemberSocial;
import com.paw.ddasoom.member.domain.SocialProvider;

public interface MemberSocialRepository extends JpaRepository<MemberSocial, Long>{
  Optional<MemberSocial> findByProviderAndProviderId(SocialProvider provider, String providerId);

}
