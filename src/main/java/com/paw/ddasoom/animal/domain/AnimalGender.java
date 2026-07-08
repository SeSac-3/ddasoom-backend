package com.paw.ddasoom.animal.domain;

import com.paw.ddasoom.animal.exception.AnimalException;

public enum AnimalGender {
  M, F, Q; // M: 남자, F: 여자, Q: 미상

  public static AnimalGender from(String value) {
    try {
      return AnimalGender.from(value);
    } catch (AnimalException e) {
      System.err.println(e.getMessage());
      return null;
    }
  }
}
