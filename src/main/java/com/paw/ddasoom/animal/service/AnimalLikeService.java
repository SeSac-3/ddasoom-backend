package com.paw.ddasoom.animal.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.paw.ddasoom.animal.exception.AnimalException;
import com.paw.ddasoom.animal.repository.AnimalRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnimalLikeService {
  /**
   * 좋아요/취소 시점에 Redis에 dirty 기록 (즉시 RDB 안씀)
   */

  private final AnimalRepository animalRepository;
  private final StringRedisTemplate redisTemplate;
  /** DIRTY_KEY의 목적: 무엇이 바뀌었는지만 표기하는 변경 추적 목록 */
  private static final String DIRTY_KEY = "animal:like:dirty";
  /** 배치 flush 중 처리 대상이 잠깐 옮겨져 있는 키 (read-your-writes 보장을 취해 함께 읽는다) */
  private static final String SNAPSHOT_KEY = "animal:like:dirty:snapshot";

  // 좋아요
  public void like(Long animalId, Long memberId) {
    try {
      if (animalRepository.existsById(animalId)) {
        redisTemplate.opsForHash().put(DIRTY_KEY, field(animalId, memberId), "1");
      }
    } catch (AnimalException e) {
      log.error("Animal iD({})는 없는 동물입니다.", animalId);
    }
  }

  // 좋아요 취소
  public void unlike(Long animalId, Long memberId) {
    try {
      if (animalRepository.existsById(animalId)) {
        redisTemplate.opsForHash().put(DIRTY_KEY, field(animalId, memberId), "0");
      }
    } catch (AnimalException e) {
      log.error("Animal iD({})는 없는 동물입니다.", animalId);
    }
  }

  /**
   * 이 회원의 "아직 RDB에 반영되지 않은" 좋아요 변경을 animalId -> liked(true/false) 맵으로 변환.
   * 조회(목록 필터/카드 상태)에서 RDB 커밋 집합 위에 덮어씌워 read-your-writes(방금 누른 좋아요 즉시 반영)를 만든다.
   * 
   * snapshot(flush 진행분) -> dirty(그 이후 변경) 순으로 병합해, 더 최신 의도가 이기게 함.
   * dirty 해시 전체를 스캔한다. flush 주기(약 10초) 동안의 전역 변경만 담겨 보통 작음.
   * 규모가 커지면 회원별 키 문리(예: animal:like:dirty:{memberId})를 검토해야 함.
   */
  public Map<Long, Boolean> getPendingLikeOverrides(Long memberId) {
    Map<Long, Boolean> overrides = new HashMap<>();
    collectOverrides(SNAPSHOT_KEY, memberId, overrides);
    collectOverrides(DIRTY_KEY, memberId, overrides);
    return overrides;
  }

  private void collectOverrides(String key, Long memberId, Map<Long, Boolean> out) {
    Map<Object, Object> entries = redisTemplate.opsForHash().entries(key); // 없는 키면 빈 맵
    for (Map.Entry<Object, Object> entry : entries.entrySet()) {
      // key값을 가져옴
      String fieldStr = (String) entry.getKey(); // "{animalId}:{memberId}"

      // ":"를 분기로 뒤에서부터 ":"의 인덱스값을 알아냄
      int separatorIndex = fieldStr.lastIndexOf(":");

      // 잘못된 값 방어
      if (separatorIndex < 0) {
        continue;
      }

      // 
      Long fieldMemberId = Long.parseLong(fieldStr.substring(separatorIndex + 1));
      if (!fieldMemberId.equals(memberId)) {
        continue;
      }

      // animalId 파실
      Long animalId = Long.parseLong(fieldStr.substring(0, separatorIndex));

      // 값 "1"=좋아요, 그 외=취소 → 맵에 담기
      out.put(animalId, "1".equals(entry.getValue()));
    }
  }

  // 키 생성 메서드
  private String field(Long animalId, Long memberId) {
    return animalId + ":" + memberId;
  }
}
