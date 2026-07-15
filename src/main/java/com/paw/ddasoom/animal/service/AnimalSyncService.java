package com.paw.ddasoom.animal.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.animal.domain.AnimalGender;
import com.paw.ddasoom.animal.domain.AnimalKind;
import com.paw.ddasoom.animal.dto.response.AnimalFetchResponse;
import com.paw.ddasoom.animal.exception.AnimalException;
import com.paw.ddasoom.animal.repository.AnimalRepository;
import com.paw.ddasoom.animal.util.AnimalDateConverter;
import com.paw.ddasoom.animal.util.AnimalNicknameGenerator;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnimalSyncService {

  private final AnimalRepository animalRepository;
  private final AnimalFetchService animalFetchService;
  private final AnimalNicknameGenerator nicknameGenerator;
  private final AnimalDateConverter dateConverter;

  @Transactional
  public List<Animal> syncAnimals() {
    List<AnimalFetchResponse.AnimalItem> items = animalFetchService.fetchAnimals();
 
    List<Animal> savedAnimals = new ArrayList<>();
    for (AnimalFetchResponse.AnimalItem item : items) {
        try {
            savedAnimals.add(upsert(item));
        } catch (AnimalException e) {
            // 값 하나가 이상하다고 8000건이 넘는 전체 동기화를 실패시키지 않는다.
            // 원본 값을 그대로 로그로 남겨서 실제로 어떤 값이 문제인지 바로 확인 가능하게 한다.
            log.warn(
                "동물 동기화 실패 - abandonmentId: {}, kind: '{}', gender: '{}', 사유: {}",
                item.abandonmentId(), item.kind(), item.gender(), e.getMessage()
            );
        }
    }
    return savedAnimals;
  }

  /**
   * abandonmentId 기준으로 이미 존재하면 업데이트, 없으면 신규 생성
   */
  private Animal upsert(AnimalFetchResponse.AnimalItem item) {
    return animalRepository.findByAbandonmentId(item.abandonmentId())
        .map(existing -> {
            existing.updateFromApi(
                item.kind(),
                item.gender(),
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
            return existing; // 영속 상태이므로 별도 save 불필요 (dirty checking)
        })
        .orElseGet(() -> animalRepository.save(toNewEntity(item)));
  }

  private Animal toNewEntity(AnimalFetchResponse.AnimalItem item) {
    return Animal.builder()
        .abandonmentId(item.abandonmentId())
        .kind(AnimalKind.from(item.kind()))
        .nickname(nicknameGenerator.generate())
        .gender(AnimalGender.from(item.gender()))
        .typeName(item.typeName())
        .age(item.age())
        .location(item.location())
        .weight(item.weight())
        .color(item.color())
        .specialMark(StringUtils.defaultIfBlank(item.specialMark(), "없음"))
        .vaccinationChk(StringUtils.defaultIfBlank(item.vaccinationChk(), "접종 안함"))
        .imageUrl(item.imageUrl())
        .rescuedAt(dateConverter.convert(item.rescuedAt()))
        .build();
  }
}
