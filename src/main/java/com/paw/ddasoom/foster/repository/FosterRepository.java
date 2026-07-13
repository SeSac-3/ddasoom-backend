package com.paw.ddasoom.foster.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.paw.ddasoom.foster.domain.Foster;

public interface FosterRepository extends JpaRepository<Foster, Long> {

  // 유저 수정 조회(유저 본인 + fosterId 조회)
  Optional<Foster> findByFosterIdAndUser_IdAndDeletedAtIsNull(Long fosterId, Long userId);

  // 유저 리스트 조회 (유저의 신청 + 삭제되지않은 신청 + 최신 신청 정렬)
  Page<Foster> findAllByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId, Pageable pageable);

}
