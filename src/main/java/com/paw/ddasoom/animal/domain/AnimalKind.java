package com.paw.ddasoom.animal.domain;

import com.paw.ddasoom.animal.exception.AnimalErrorCode;
import com.paw.ddasoom.animal.exception.AnimalException;

public enum AnimalKind {
  D, C; // D: 개, C: 고양이

  public static AnimalKind from(String value) {
    return switch(value) {
      case "개" -> D;
      case "고양이" -> C;
      default -> throw new AnimalException(AnimalErrorCode.ANIMAL_ENUM_VALUE_NOT_FOUND);
    };
  }
}
