package com.paw.ddasoom.animal.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnimalLikeService {
  /**
   * 좋아요/취소 시점에 Redis에 dirty 기록 (즉시 RDB 안씀)
   */

  private final StringRedisTemplate redisTemplate;
  /** DIRTY_KEY의 목적: 무엇이 바뀌었는지만 표기하는 변경 추적 목록 */
  private static final String DIRTY_KEY = "animal:like:dirty";

  // 좋아요
  public void like(Long animalId, Long memberId) {
    redisTemplate.opsForHash().put(DIRTY_KEY, field(animalId, memberId), "1");
    // redisTemplate.opsForValue().increment("animal:like:count:" + animalId); // 실시간 표시용으로 활용 시 사용
  }

  // 좋아요 취소
  public void unlike(Long animalId, Long memberId) {
    redisTemplate.opsForHash().put(DIRTY_KEY, field(animalId, memberId), "0");
    // redisTemplate.opsForValue().decrement("animal:like:count:" + animalId); // 실시간 표시용으로 활용 시 사용
  }

  // 키 생성 메서드
  private String field(Long animalId, Long memberId) {
    return animalId + ":" + memberId;
  }
}
