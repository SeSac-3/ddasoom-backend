package com.paw.ddasoom.support.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.paw.ddasoom.support.domain.Notice;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

  // 유저용 - 노출 + 미삭제, 고정글(오름차순), 일반글(오름차순)
  @Query("""
        SELECT n FROM Notice n
        WHERE n.deletedAt IS NULL
          AND n.isVisible = true
        ORDER BY n.pinOrder ASC NULLS LAST, n.createdAt DESC
        """)
  Page<Notice> findAllForUser (Pageable pageable);

  // 관리자용 — 노출여부 무관 전체, 정렬 로직 유저용과 동일
  @Query("""
        SELECT n FROM Notice n
        WHERE n.deletedAt IS NULL
        ORDER BY n.pinOrder ASC NULLS LAST, n.createdAt DESC
        """)
    Page<Notice> findAllForAdmin(Pageable pageable);

  // 상세조회
  Optional<Notice> findByIdAndDeletedAtIsNull(Long id);

  // 고정처리된 공지사항 목록만 조회 (오름차순)
  @Query("""
      SELECT n FROM Notice n
      WHERE n.deletedAt IS NULL
        AND n.pinOrder IS NOT NULL
      ORDER BY n.pinOrder ASC
      """)
    List<Notice> findAllPinned();

  // 재정렬(reorderPinned)용 — 요청받은 ID들 중 삭제되지 않은 공지만 일괄 조회 (N+1 방지)
  List<Notice> findAllByIdInAndDeletedAtIsNull(List<Long> ids);
}
