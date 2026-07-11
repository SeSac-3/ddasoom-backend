package com.paw.ddasoom.animal.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

@Component
public class AnimalNicknameGenerator {

  /**
   * 랜덤으로 동물의 닉네임 이름 리스트로 저장
   */
  private static final List<String> NOUNS = List.of(
    "몽이", "콩이", "보리", "두부", "초코", "별이", "구름이", "하늘이"
  );

  /**
   * 동물들의 이름을 생성
   * @return 이름
   */
  public String generate() {
    String noun = pick(NOUNS);
    return noun;
  }

  /**
   * 동물 이름 리스트 안의 랜덤 인덱스 값 뽑음
   * @param list 동물 이름 리스트
   * @return 리스트 중 랜덤 인덱스의 값
   */
  private String pick(List<String> list) {
    int index = ThreadLocalRandom.current().nextInt(list.size());
    return list.get(index);
  }
}
