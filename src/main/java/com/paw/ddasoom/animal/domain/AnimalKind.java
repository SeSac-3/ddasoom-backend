package com.paw.ddasoom.animal.domain;

import com.paw.ddasoom.animal.exception.AnimalException;

public enum AnimalKind {
  D, C; // D: 개, C: 고양이

  public static AnimalKind from(String value) {
    try {
      return AnimalKind.from(value);
    } catch (AnimalException e) {
      System.err.println(e.getMessage());
      return null;
    }
  }
}
