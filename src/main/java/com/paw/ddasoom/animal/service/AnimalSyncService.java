package com.paw.ddasoom.animal.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.animal.batch.AnimalSyncItem;
import com.paw.ddasoom.animal.domain.AnimalGender;
import com.paw.ddasoom.animal.domain.AnimalKind;
import com.paw.ddasoom.animal.dto.response.AnimalFetchResponse;
import com.paw.ddasoom.animal.exception.AnimalException;
import com.paw.ddasoom.animal.repository.AnimalSyncJdbcRepository;
import com.paw.ddasoom.animal.util.AnimalDateConverter;
import com.paw.ddasoom.animal.util.AnimalNicknameGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AnimalSyncService {
 
  private final AnimalFetchService animalFetchService;
  private final AnimalSyncJdbcRepository animalSyncJdbcRepository;
  private final AnimalNicknameGenerator nicknameGenerator;
  private final AnimalDateConverter dateConverter;
 
  /**
   * @return 이번 동기화에서 실제로 upsert된(=검증을 통과한) 건수
   */
  public int syncAnimals() {
    
    List<AnimalFetchResponse.AnimalItem> items = animalFetchService.fetchAnimals();
 
    List<AnimalSyncItem> validItems = new ArrayList<>(items.size());
    Map<String, Integer> kindErrorCounts = new HashMap<>();
    Map<String, Integer> genderErrorCounts = new HashMap<>();

    for (AnimalFetchResponse.AnimalItem item : items) {
      try {
        validItems.add(toSyncItem(item));
      } catch (AnimalException e) {
        // 값 하나가 이상하다고 8000건이 넘는 전체 동기화를 실패시키지 않는다.
        // 원본 값(kind, gender)과 실패 사유를 그대로 로그로 남겨서 어떤 값이 문제인지 확인 가능하게 한다.
        kindErrorCounts.merge(item.kind(), 1, Integer::sum);
        genderErrorCounts.merge(item.gender(), 1, Integer::sum);
      }
    }
 
    animalSyncJdbcRepository.bulkUpsert(validItems);
 
    log.info("동물 동기화 완료 - 유효 {}건 / 전체 {}건 upsert", validItems.size(), items.size());
    kindErrorCounts.forEach((kind, count) ->
      log.info("동기화 실패 사유(kind) - {}: {}건", kind, count));
    genderErrorCounts.forEach((gender, count) ->
      log.info("동기화 실패 사유(gender) - {}: {}건", gender, count));
    return validItems.size();
  }
 
  /**
   * 공공API 원본 아이템을 검증/변환해 벌크 upsert용 아이템으로 만든다.
   * enum 변환(AnimalKind.from/AnimalGender.from) 또는 날짜 파싱(dateConverter.convert)이 실패하면
   * AnimalException이 던져지고, 호출부(syncAnimals)에서 해당 건만 건너뛴다.
   */
  private AnimalSyncItem toSyncItem(AnimalFetchResponse.AnimalItem item) {
    return new AnimalSyncItem(
        item.abandonmentId(),
        AnimalKind.from(item.kind()),
        nicknameGenerator.generate(), // 신규 INSERT일 때만 반영(기존 행이면 SQL이 기존 닉네임 유지)
        AnimalGender.from(item.gender()),
        item.typeName(),
        item.age(),
        item.location(),
        item.weight(),
        item.color(),
        StringUtils.defaultIfBlank(item.specialMark(), "없음"),
        StringUtils.defaultIfBlank(item.vaccinationChk(), "접종 안함"),
        item.imageUrl(),
        dateConverter.convert(item.rescuedAt())
    );
  }
}