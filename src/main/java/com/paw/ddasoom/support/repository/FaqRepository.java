package com.paw.ddasoom.support.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.paw.ddasoom.support.domain.Faq;

public interface FaqRepository extends JpaRepository<Faq, Long>{

  // 유저용 = 노출 + 미삭제, 카테고리순 정렬
  @Query("""
      SELECT f FROM Faq f
      WHERE f.deletedAt IS NULL AND f.isVisible = true
      ORDER BY f.category ASC, f.createdAt DESC
      """)
  List<Faq> findAllForUser();
  
  // 관리자용 - 노출여부 무관 전체
  @Query("""
      SELECT f FROM Faq f
      WHERE f.deletedAt IS NULL
      ORDER BY f.category ASC, f.createdAt DESC
      """)
  List<Faq> findAllForAdmin();

  Optional<Faq> findByIdAndDeletedAtIsNull(Long id);
}
