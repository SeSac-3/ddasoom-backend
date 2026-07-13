package com.paw.ddasoom.foster.dto.response;

import java.time.LocalDateTime;

import com.paw.ddasoom.foster.domain.Foster;
import com.paw.ddasoom.foster.domain.FosterStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FosterUserListResponse {

  private Long fosterId;
  private Long animalId;
  private String animalNickname;
  private String animalImageUrl;
  private FosterStatus status;
  private LocalDateTime createdAt;

  public static FosterUserListResponse from(Foster foster) {
    return FosterUserListResponse.builder()
        .fosterId(foster.getFosterId())
        .animalId(foster.getAnimal().getId())
        .animalNickname(foster.getAnimal().getNickname())
        .animalImageUrl(foster.getAnimal().getImageUrl())
        .status(foster.getStatus())
        .createdAt(foster.getCreatedAt())
        .build();
  }

}
