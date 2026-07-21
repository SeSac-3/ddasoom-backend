package com.paw.ddasoom.animal.repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.paw.ddasoom.animal.batch.AnimalSyncItem;

import lombok.RequiredArgsConstructor;

/**
 * 공공API 유기동물 동기화 - JdbcTemplate 벌크 upsert (영속성 컨텍스트 안 거침).
 *
 * AnimalSyncService가 건별로 findByAbandonmentId 조회 후 save/dirty-checking 하던 방식(N+1)을
 * INSERT ... ON DUPLICATE KEY UPDATE 한 번(또는 배치 분할)으로 대체한다.
 *
 * ⚠️ like_count / is_fostered / created_at / updated_at 은 이 SQL이 절대 건드리지 않는다.
 *   - like_count, is_fostered: 다른 도메인(좋아요 배치, foster)이 소유한 캐시 컬럼 -> INSERT 시 DEFAULT(0/false)만 적용
 *   - created_at, updated_at: DB가 관리(DEFAULT / ON UPDATE CURRENT_TIMESTAMP(6)) -> SQL에서 값 세팅 안 함
 *   - nickname: UPDATE 절에 없음 -> 신규 INSERT일 때만 세팅, 기존 행이면 유지
 */
@Repository
@RequiredArgsConstructor
public class AnimalSyncJdbcRepository {
 
  private final JdbcTemplate jdbcTemplate;
 
  // 한 번에 실행할 배치 크기. items 전체를 한 배치로 넘기면 대량(수천~수만 건) 동기화 시
  // 패킷/메모리 부담이 커질 수 있어, JdbcTemplate이 내부적으로 이 크기 단위로 나눠 executeBatch 한다.
  private static final int BATCH_SIZE = 1000;
 
  public void bulkUpsert(List<AnimalSyncItem> items) {
    if (items.isEmpty()) {
      return;
    }
 
    String sql = """
      INSERT INTO animal
        (abandonment_id, kind, nickname, gender, type_name, age, location,
         weight, color, special_mark, vaccination_chk, image_url, rescued_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE
        kind = VALUES(kind),
        gender = VALUES(gender),
        type_name = VALUES(type_name),
        age = VALUES(age),
        location = VALUES(location),
        weight = VALUES(weight),
        color = VALUES(color),
        special_mark = VALUES(special_mark),
        vaccination_chk = VALUES(vaccination_chk),
        image_url = VALUES(image_url),
        rescued_at = VALUES(rescued_at)
      """;
 
    jdbcTemplate.batchUpdate(sql, items, BATCH_SIZE, (ps, item) -> {
      ps.setString(1, item.abandonmentId());
      ps.setString(2, item.kind().name());       // AnimalKind enum -> "D"/"C" (VARCHAR(20) 컬럼)
      ps.setString(3, item.nickname());
      ps.setString(4, item.gender().name());      // AnimalGender enum -> "M"/"F"/"Q" (CHAR(1) 컬럼)
      ps.setString(5, item.typeName());
      ps.setString(6, item.age());
      ps.setString(7, item.location());
      ps.setString(8, item.weight());
      ps.setString(9, item.color());
      ps.setString(10, item.specialMark());
      ps.setString(11, item.vaccinationChk());
      ps.setString(12, item.imageUrl());
      setNullableTimestamp(ps, 13, item.rescuedAt());
    });
  }
 
  private void setNullableTimestamp(java.sql.PreparedStatement ps, int index, LocalDateTime value)
      throws java.sql.SQLException {
    if (value != null) {
      ps.setTimestamp(index, Timestamp.valueOf(value));
    } else {
      ps.setNull(index, Types.TIMESTAMP);
    }
  }
}