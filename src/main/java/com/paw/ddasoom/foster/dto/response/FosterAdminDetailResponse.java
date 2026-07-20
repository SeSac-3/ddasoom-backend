package com.paw.ddasoom.foster.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.paw.ddasoom.foster.domain.Foster;
import com.paw.ddasoom.foster.domain.FosterStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FosterAdminDetailResponse {

  private Long fosterId;
  private UUID fosterNum;

  private Long animalId;
  private String animalNickname;
  private String animalImageUrl;

  private Long userId;
  private String userEmail;
  private String userNickname;
  private String userTel;

  private Long reviewerId;
  private String reviewerNickname;

  private String age;
  private String job;
  private String message;

  private String answer;
  private FosterStatus status;

  private LocalDateTime fosterStartAt;
  private LocalDateTime fosterEndAt;
  private LocalDateTime fosterExtendAt;
  private LocalDateTime fosterCompleteAt;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  public static FosterAdminDetailResponse from(Foster foster) {
    return FosterAdminDetailResponse.builder()
        .fosterId(foster.getId())
        .fosterNum(foster.getFosterNum())
        .animalId(foster.getAnimal().getId())
        .animalNickname(foster.getAnimal().getNickname())
        .animalImageUrl(foster.getAnimal().getImageUrl())
        .userId(foster.getUser().getId())
        .userEmail(foster.getUser().getEmail())
        .userNickname(foster.getUser().getNickname())
        .userTel(foster.getUser().getTel())
        .reviewerId(foster.getReviewer() != null ? foster.getReviewer().getId() : null)
        .reviewerNickname(foster.getReviewer() != null ? foster.getReviewer().getNickname() : null)
        .age(foster.getAge())
        .job(foster.getJob())
        .message(foster.getMessage())
        .answer(foster.getAnswer())
        .status(foster.getStatus())
        .fosterStartAt(foster.getFosterStartAt())
        .fosterEndAt(foster.getFosterEndAt())
        .fosterExtendAt(foster.getFosterExtendAt())
        .fosterCompleteAt(foster.getFosterCompleteAt())
        .createdAt(foster.getCreatedAt())
        .updatedAt(foster.getUpdatedAt())
        .deletedAt(foster.getDeletedAt())
        .build();
  }
}
