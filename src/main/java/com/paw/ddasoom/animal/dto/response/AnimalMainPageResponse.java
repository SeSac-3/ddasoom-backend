// package com.paw.ddasoom.animal.dto.response;

// import com.paw.ddasoom.animal.domain.Animal;
// import com.paw.ddasoom.animal.domain.AnimalGender;
// import com.paw.ddasoom.animal.domain.AnimalKind;

// public record AnimalMainPageResponse(
//   Long id,
//   String abandonmentId,
//   AnimalKind kind, // 상위 품종 분류 (개/고양이 등)
//   String nickname,
//   AnimalGender gender,
//   String typeName, // 품종 이름
//   String age, // 출생 연도 (예: 2026(년도))
//   String location,
//   String weight, // 몸무게 (kg)
//   String color,
//   String specialMark,
//   String vaccinationChk,
//   String imageUrl,
//   long likeCount // 캐시 컬럼 - animal_like 기준 동기화
// ) {
//   public static AnimalMainPageResponse from(Animal animal) {
//     return new AnimalMainPageResponse(
//       animal.getId(),
//       animal.getAbandonmentId(), 
//       animal.getKind(), 
//       animal.getNickname(), 
//       animal.getGender(), 
//       animal.getTypeName(), 
//       animal.getAge(), 
//       animal.getLocation(), 
//       animal.getWeight(), 
//       animal.getColor(), 
//       animal.getSpecialMark(), 
//       animal.getVaccinationChk(), 
//       animal.getImageUrl(), 
//       animal.getLikeCount());
//   }
// }
