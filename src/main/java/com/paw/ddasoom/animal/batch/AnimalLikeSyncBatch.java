package com.paw.ddasoom.animal.batch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.paw.ddasoom.animal.repository.AnimalLikeJdbcRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnimalLikeSyncBatch {
  /**
   * 토글 최종 상태만 해시에 남김
   * flush 주기 안에서 여러번 눌러도 마지막 상태 하나만 반영됨
   * 주기적으로 dirty를 읽어 RDB에 반영
   */

  private final StringRedisTemplate redisTemplate;
  private final AnimalLikeJdbcRepository jdbcRepository;
  private static final String DIRTY_KEY = "animal:like:dirty";
  private static final String SNAPSHOT_KEY = "animal:like:dirty:snapshot";

  @Scheduled(fixedDelay = 10_000) // 10초 간격 갱신
  @Transactional   // insert+delete+count 갱신을 한 트랜잭션으로
  public void flush() {

    // 스냅샷 삭제를 DB 커밋 후에 실행하게 설정.
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override public void afterCommit() { redisTemplate.delete(SNAPSHOT_KEY); }
    });

    // 이전 실행이 실패로 남긴 스냅샷이 있으면 그것부터, 없으면 현재 dirty를 원자적으로 스왑
    if (Boolean.FALSE.equals(redisTemplate.hasKey(SNAPSHOT_KEY))) { // 현재 스냅샷 키가 없으면
      if (Boolean.FALSE.equals(redisTemplate.hasKey(DIRTY_KEY))) return;   // 현재 스탭샷 키가 없고, 더티키가 없으면 -> 할 일 없음
      redisTemplate.rename(DIRTY_KEY, SNAPSHOT_KEY);   // 현재 스냅샷 키가 없고, 더티키가 있으면 -> 이 순간 이후 클릭은 새 DIRTY_KEY로 쌓임
    }

    // 스냅샷 키가 있으면 기존 스냅샷을 현재 더티에 적용
    Map<Object, Object> dirty = redisTemplate.opsForHash().entries(SNAPSHOT_KEY);

    List<AnimalLikeSyncItem> toInsert = new ArrayList<>();
    List<AnimalLikeSyncItem> toDelete = new ArrayList<>();
    Set<Long> affectedAnimalIds = new HashSet<>();

    // 더티키를 ":"를 분기로 animalId, memberId, value값을 알아내고, value값에 따라 insert/delete로 나눔
    for (Map.Entry<Object, Object> e : dirty.entrySet()) {
      String[] parts = ((String) e.getKey()).split(":");
      Long animalId = Long.valueOf(parts[0]);
      Long memberId = Long.valueOf(parts[1]);
      affectedAnimalIds.add(animalId);
      if ("1".equals(e.getValue())) toInsert.add(new AnimalLikeSyncItem(animalId, memberId));
      else                          toDelete.add(new AnimalLikeSyncItem(animalId, memberId));
    }
    doFlush(toInsert, toDelete, affectedAnimalIds);
  }

  private void doFlush(
    List<AnimalLikeSyncItem> toInsert,
    List<AnimalLikeSyncItem> toDelete,
    Set<Long> affectedAnimalIds) {
      
      if (toInsert.isEmpty() == false) jdbcRepository.bulkInsertIgnore(toInsert);
      if (toDelete.isEmpty() == false) jdbcRepository.bulkDelete(toDelete);

      // 좋아요 누른 동물의 좋아요 수를 animal 테이블에 저장
      jdbcRepository.updateLikedAnimalCounts(affectedAnimalIds);
  }
}
