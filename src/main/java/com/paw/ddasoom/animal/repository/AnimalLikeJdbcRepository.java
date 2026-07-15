package com.paw.ddasoom.animal.repository;

import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.paw.ddasoom.animal.service.AnimalLikeSyncItem;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AnimalLikeJdbcRepository {
  /**
   * Bulk Insert / Delete - JdbcTemplate (영속성 컨텍스트 안 거침)
   */

  private final JdbcTemplate jdbcTemplate;

  /**
   * INSERT IGNORE - UNIQUE(animal_id, member_id) 중복이면 무시 (재좋아요 안전)
   * 물리 삭제 후 복구 로직이 없음
   * items가 100건이면, 하나의 SQL틀에 파라미터만 100개를 갈아끼워 100번의 INSERT를 전부 실행
   * rewriteBatchedStatements=true 가 하는 일이 여기서 발생
   * 물리적으로 한번에 DB에 저장
   */ 
  public void bulkInsertIgnore(List<AnimalLikeSyncItem> items) {
    String sql = """
      INSERT IGNORE INTO animal_like (animal_id, member_id, created_at, updated_at)
      VALUES (?, ?, NOW(6), NOW(6))
      """;
      jdbcTemplate.batchUpdate(sql, items, items.size(),
        (ps, item) -> {
          ps.setLong(1, item.animalId());
          ps.setLong(2, item.memberId());
        });
  }

  // 물리 삭제 (query delete)
  public void bulkDelete(List<AnimalLikeSyncItem> items) {
    String sql = "DELETE FROM animal_like WHERE animal_id = ? AND member_id = ?";
    jdbcTemplate.batchUpdate(sql, items, items.size(),
      (ps, item) -> {
        ps.setLong(1, item.animalId());
        ps.setLong(2, item.memberId());
      });
  }

  // 좋아요 수 업데이트
  public void updateLikedAnimalCounts(Set<Long> items) {
    String sql = """
        UPDATE animal a SET a.like_count = (
          SELECT COUNT(*) FROM animal_like al
          WHERE al.animal_id = a.animal_id
        )
        WHERE a.animal_id = ?
        """;
    jdbcTemplate.batchUpdate(sql, items, items.size(),
      (ps, item) -> {
        ps.setLong(1, item);
      });
  }
}
