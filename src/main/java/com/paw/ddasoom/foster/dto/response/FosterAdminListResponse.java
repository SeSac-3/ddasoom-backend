package com.paw.ddasoom.foster.dto.response;

import java.time.LocalDateTime;

import com.paw.ddasoom.foster.domain.Foster;
import com.paw.ddasoom.foster.domain.FosterStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FosterAdminListResponse {

  private Long fosterId;

  private Long animalId;
  private String animalNickname;
  private String animalImageUrl;

  private Long userId;
  private String userNickname;

  private Long reviewerId;
  private String reviewerNickname;

  private FosterStatus status;

  private LocalDateTime fosterStartAt;
  private LocalDateTime fosterEndAt;
  private LocalDateTime fosterExtendAt;
  private LocalDateTime fosterCompleteAt;


  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;


  public static FosterAdminListResponse from(Foster foster) {
    return FosterAdminListResponse.builder()
        .fosterId(foster.getId())
        .animalId(foster.getAnimal().getId())
        .animalNickname(foster.getAnimal().getNickname())
        .animalImageUrl(foster.getAnimal().getImageUrl())
        .userId(foster.getUser().getId())
        .userNickname(foster.getUser().getNickname())
        .reviewerId(foster.getReviewer() != null ? foster.getReviewer().getId() : null)
        .reviewerNickname(foster.getReviewer() != null ? foster.getReviewer().getNickname() : null)
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