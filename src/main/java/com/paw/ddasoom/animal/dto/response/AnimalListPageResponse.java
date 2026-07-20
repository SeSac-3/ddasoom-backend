package com.paw.ddasoom.animal.dto.response;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.animal.domain.AnimalGender;
import com.paw.ddasoom.animal.domain.AnimalKind;

import lombok.Builder;

@Builder
public record AnimalListPageResponse(
  Long animalId,
  AnimalKind kind,
  String nickname,
  AnimalGender gender,
  String typeName,
  String age,
  String imageUrl,
  String location,
  boolean isFostered,
  int likeCount,
  boolean isLiked // 로그인 회원이 좋아요한 동물인지 - RDB animal_like 기준(비로그인이면 항상 false)
) {
  // isLiked는 회원별로 달라지므로 엔티티만으로는 판단 불가 -> 서비스에서 계산해 주입
  public static AnimalListPageResponse from(Animal animal, boolean isLiked) {
    return AnimalListPageResponse.builder()
      .animalId(animal.getId())
      .kind(animal.getKind())
      .nickname(animal.getNickname())
      .gender(animal.getGender())
      .typeName(animal.getTypeName())
      .age(animal.getAge())
      .imageUrl(animal.getImageUrl())
      .location(animal.getLocation())
      .isFostered(animal.isFostered())
      .likeCount(animal.getLikeCount())
      .isLiked(isLiked)
      .build();
  }
}
