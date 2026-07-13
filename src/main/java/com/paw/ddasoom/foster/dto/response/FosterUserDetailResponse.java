package com.paw.ddasoom.foster.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.paw.ddasoom.foster.domain.Foster;
import com.paw.ddasoom.foster.domain.FosterStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FosterUserDetailResponse {

  private Long fosterId;
  private Long animalId;
  private String animalNickname;
  private String animalImageUrl;

  private String age;
  private String job;
  private String message;

  private String answer;
  private FosterStatus status;
  private UUID fosterNum;

  private LocalDateTime fosterStartAt;
  private LocalDateTime fosterEndAt;
  private LocalDateTime fosterExtendAt;
  private LocalDateTime fosterCompleteAt;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static FosterUserDetailResponse from(Foster foster) {
    return FosterUserDetailResponse.builder()
        .fosterId(foster.getFosterId())
        .animalId(foster.getAnimal().getId())
        .animalNickname(foster.getAnimal().getNickname())
        .animalImageUrl(foster.getAnimal().getImageUrl())
        .age(foster.getAge())
        .job(foster.getJob())
        .message(foster.getMessage())
        .answer(foster.getAnswer())
        .status(foster.getStatus())
        .fosterNum(foster.getFosterNum())
        .fosterStartAt(foster.getFosterStartAt())
        .fosterEndAt(foster.getFosterEndAt())
        .fosterExtendAt(foster.getFosterExtendAt())
        .fosterCompleteAt(foster.getFosterCompleteAt())
        .createdAt(foster.getCreatedAt())
        .updatedAt(foster.getUpdatedAt())
        .build();
  }
}
