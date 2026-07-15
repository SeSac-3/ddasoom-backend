package com.paw.ddasoom.animal.dto.request;

import jakarta.validation.constraints.NotNull;

public record AnimalLikeRequest(
  @NotNull
  Long animalId
) {}
