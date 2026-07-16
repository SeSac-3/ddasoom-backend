package com.paw.ddasoom.animal.dto.response;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.animal.domain.AnimalGender;
import com.paw.ddasoom.animal.domain.AnimalKind;

import lombok.Builder;

@Builder
public record AnimalDetailPageResponse(
  Long animalId,
  AnimalKind kind,
  String nickname,
  AnimalGender gender,
  String typeName,
  String age,
  String location,
  String weight,
  String color,
  String specialMark,
  String vaccinationChk,
  String imageUrl,
  boolean isFostered,
  int likeCount,
  boolean isLiked // 로그인 회원이 좋아요한 동물인지 - 비로그인이면 항상 false
) {
  public static AnimalDetailPageResponse from(Animal animal, boolean isLiked) {
    return AnimalDetailPageResponse.builder()
      .animalId(animal.getId())
      .kind(animal.getKind())
      .nickname(animal.getNickname())
      .gender(animal.getGender())
      .typeName(animal.getTypeName())
      .age(animal.getAge())
      .location(animal.getLocation())
      .weight(animal.getWeight())
      .color(animal.getColor())
      .specialMark(animal.getSpecialMark())
      .vaccinationChk(animal.getVaccinationChk())
      .imageUrl(animal.getImageUrl())
      .isFostered(animal.isFostered())
      .likeCount(animal.getLikeCount())
      .isLiked(isLiked)
      .build();
    }
}
