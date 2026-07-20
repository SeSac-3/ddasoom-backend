package com.paw.ddasoom.animal.batch;

import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.paw.ddasoom.animal.service.AnimalSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 공공데이터포털 유기동물 데이터 정기 미러링 스케줄러.
 *
 * 관리자 수동 sync(AdminAnimalController)와 "완전히 같은" AnimalSyncService.syncAnimals()를 호출한다.
 * 즉 트리거만 둘(수동 버튼 / 타이머)이고 동기화 로직은 한 곳에 모여 있다.
 *
 * 주기: 매일 새벽 4시(KST) — 원본이 일 단위로 갱신되고 전체 순회가 무거워 하루 1회가 적정.
 *       (하루 2회로 늘리려면 cron = "0 0 4,16 * * *")
 *
 * @Scheduled 메서드는 컨트롤러/HTTP와 무관하게 스프링 스케줄러 스레드가 직접 호출한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnimalSyncScheduler {

  private final AnimalSyncService animalSyncService;

  @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
  public void scheduledSync() {
    log.info("[AnimalSyncScheduler] 유기동물 정기 동기화 시작");
    try {
      int savedCount = animalSyncService.syncAnimals().size();
      log.info("[AnimalSyncScheduler] 정기 동기화 완료 - {}건 저장/갱신", savedCount);
    } catch (DataAccessException e) {
      // 예외를 흡수하지 않으면 스케줄러 스레드가 죽어 이후 실행이 멈춘다.
      // 로그만 남기고 다음 주기에 다시 시도하도록 둔다.
      log.error("[AnimalSyncScheduler] 정기 동기화 실패 - 다음 주기에 재시도", e);
    }
  }
}
