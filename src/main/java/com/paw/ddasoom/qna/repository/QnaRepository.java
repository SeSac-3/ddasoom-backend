package com.paw.ddasoom.qna.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.paw.ddasoom.qna.domain.Qna;
import com.paw.ddasoom.qna.domain.QnaStatus;

public interface QnaRepository extends JpaRepository<Qna, Long> {

  // 1) 상세 조회: 소유권 검증 및 상세 응답 시 사용 (Fetch Join으로 한 번에 가져옴)
  @EntityGraph(attributePaths = "questioner")
  Optional<Qna> findByIdAndDeletedAtIsNull(Long id);

  // 2) 유저용: 본인 문의 목록 조회 (idx_qna_questioner_created 복합 인덱스 매핑)
  @EntityGraph(attributePaths = "questioner")
  Page<Qna> findByQuestioner_IdAndDeletedAtIsNullOrderByCreatedAtDesc(Long questionerId, Pageable pageable);

  // 3) 관리자용 (전체): 상태 미지정 전체 목록 (deleted_at 및 created_at DESC 조건 활용)
  @EntityGraph(attributePaths = "questioner")
  Page<Qna> findByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);

  // 4) 관리자용 (필터): 특정 상태 필터링 목록 (idx_qna_status_created 복합 인덱스 정밀 조준 🎯)
  @EntityGraph(attributePaths = "questioner")
  Page<Qna> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(QnaStatus status, Pageable pageable);
}