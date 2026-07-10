package com.paw.ddasoom.member.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.domain.Role;

public interface MemberRepository extends JpaRepository<Member, Long>{
  Optional<Member> findByEmail(String email);
  boolean existsByEmail(String email);
  boolean existsByNickname(String nickname);

  /** 관리자 목록 검색 — keyword(이메일/닉네임 부분일치), role. 탈퇴 회원 포함 전체, 가입일 최신순 */
  @Query("""
          SELECT m FROM Member m
          WHERE (:keyword IS NULL OR m.email LIKE %:keyword% OR m.nickname LIKE %:keyword%)
            AND (:role IS NULL OR m.role = :role)
          ORDER BY m.createdAt DESC
          """)
  Page<Member> searchForAdmin(@Param("keyword") String keyword,
                              @Param("role") Role role,
                              Pageable pageable);
}
