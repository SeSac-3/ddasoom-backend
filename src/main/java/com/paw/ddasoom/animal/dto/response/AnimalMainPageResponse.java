package com.paw.ddasoom.animal.dto.response;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.animal.domain.AnimalGender;
import com.paw.ddasoom.animal.domain.AnimalKind;

import lombok.Builder;

@Builder
// DTO 확인용
public record AnimalMainPageResponse(
  long animalId,
  String abandonmentId,
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
  long likeCount,
  boolean isFostered
) {
  public static AnimalMainPageResponse from(Animal animal) {
    return AnimalMainPageResponse.builder()
      .animalId(animal.getId())
      .abandonmentId(animal.getAbandonmentId())
      .kind(animal.getKind())
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
      .likeCount(animal.getLikeCount())
      .isFostered(animal.isFostered())
      .build();
  }
}
