package com.paw.ddasoom.animal.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.paw.ddasoom.animal.exception.AnimalErrorCode;
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
  /** 배치 flush 중 처리 대상이 잠깐 옮겨져 있는 키 (read-your-writes 보장을 위해 함께 읽는다) */
  private static final String SNAPSHOT_KEY = "animal:like:dirty:snapshot";

  // 좋아요
  public void like(Long animalId, Long memberId) {
    try {
      if (animalRepository.existsById(animalId) == false) {
        throw new AnimalException(AnimalErrorCode.ANIMAL_NOT_FOUND);
      }
      redisTemplate.opsForHash().put(DIRTY_KEY, field(animalId, memberId), "1");
    } catch (AnimalException e) {
      log.error("Animal iD({})는 없는 동물입니다.", animalId);
    }
  }

  // 좋아요 취소
  public void unlike(Long animalId, Long memberId) {
    try {
      if (animalRepository.existsById(animalId) == false) {
        throw new AnimalException(AnimalErrorCode.ANIMAL_NOT_FOUND);
      }
      redisTemplate.opsForHash().put(DIRTY_KEY, field(animalId, memberId), "0");
    } catch (AnimalException e) {
      log.error("Animal iD({})는 없는 동물입니다.", animalId);
    }
  }

  /**
   * 이 회원의 "아직 RDB에 반영되지 않은" 좋아요 변경 전체를 animalId -> liked(true/false) 맵으로 반환.
   * "좋아요만" 필터처럼 대상 animalId를 미리 알 수 없을 때(전체 dirty를 훑어야 할 때) 사용한다.
   */
  public Map<Long, Boolean> getPendingLikeOverrides(Long memberId) {
    Map<Long, Boolean> overrides = new HashMap<>();
    collectOverrides(SNAPSHOT_KEY, memberId, overrides);
    collectOverrides(DIRTY_KEY, memberId, overrides);
    return overrides;
  }

  /**
   * 이 회원의 "아직 RDB에 반영되지 않은" 좋아요 변경을 animalId -> liked(true/false) 맵으로 변환.
   * 조회(목록 필터/카드 상태)에서 RDB 커밋 집합 위에 덮어씌워 read-your-writes(방금 누른 좋아요 즉시 반영)를 만든다.
   * 
   * snapshot(flush 진행분) -> dirty(그 이후 변경) 순으로 병합해, 더 최신 의도가 이기게 함.
   * dirty 해시 전체를 스캔한다. flush 주기(약 10초) 동안의 전역 변경만 담겨 보통 작음.
   * 규모가 커지면 회원별 키 분리(예: animal:like:dirty:{memberId})를 검토해야 함.
   */
  public Map<Long, Boolean> getPendingLikeOverrides(Long memberId, List<Long> animalIds) {

    if (animalIds.isEmpty()) {
      return Map.of();
    }

    List<Object> fields = new ArrayList<>(animalIds.size());

    for (Long animalId : animalIds) {
      fields.add(field(animalId, memberId));
    }

    List<Object> snapshotValues = redisTemplate.opsForHash().multiGet(SNAPSHOT_KEY, fields);
    List<Object> dirtyValues = redisTemplate.opsForHash().multiGet(DIRTY_KEY, fields);

    Map<Long, Boolean> overrides = new HashMap<>();

    for (int i = 0; i < animalIds.size(); i++) {
      // dirty가 snapshot보다 최신 의도이므로 우선 적용
      Object value = dirtyValues.get(i) != null ? dirtyValues.get(i) : snapshotValues.get(i);

      if (value != null) {
        overrides.put(animalIds.get(i), "1".equals(value));
      }
    }
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

      Long fieldMemberId;
      Long animalId;
      try {
        // memberId 파싱
        fieldMemberId = Long.parseLong(fieldStr.substring(separatorIndex + 1));
        if (fieldMemberId.equals(memberId) == false) {
          continue;
        }
  
        // animalId 파싱
        animalId = Long.parseLong(fieldStr.substring(0, separatorIndex));

        // 값 "1"=좋아요, 그 외=취소 → 맵에 담기
      } catch (NumberFormatException e) {
        continue; // 형식이 깨진 field는 건너뜀 (전체 조회가 500으로 죽지 않도록)
      }
      out.put(animalId, "1".equals(entry.getValue()));
    }
  }

  // 키 생성 메서드
  private String field(Long animalId, Long memberId) {
    return animalId + ":" + memberId;
  }
}
