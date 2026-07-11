package com.paw.ddasoom.animal.domain;

import com.paw.ddasoom.animal.exception.AnimalErrorCode;
import com.paw.ddasoom.animal.exception.AnimalException;

public enum AnimalGender {
  M, F, Q; // M: 남자, F: 여자, Q: 미상

  public static AnimalGender from(String value) {
    return switch(value) {
      case "M" -> M;
      case "F" -> F;
      case "Q" -> Q;
      default -> throw new AnimalException(AnimalErrorCode.ANIMAL_ENUM_VALUE_NOT_FOUND);
    };
  }
}
