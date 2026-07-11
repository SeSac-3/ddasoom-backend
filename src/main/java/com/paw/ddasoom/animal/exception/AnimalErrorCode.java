package com.paw.ddasoom.animal.exception;

import org.springframework.http.HttpStatus;

import com.paw.ddasoom.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnimalErrorCode implements ErrorCode{

  ANIMAL_NOT_FOUND(HttpStatus.NOT_FOUND, "ANIMAL_001", "해당 동물을 찾을 수 없습니다."),
  ANIMAL_ENUM_VALUE_NOT_FOUND(HttpStatus.NOT_FOUND, "ANIMAL_002", "해당 동물의 종류를 찾을 수 없습니다."),
  ANIMAL_NAME_INVALID(HttpStatus.NOT_ACCEPTABLE, "ANIMAL_003", "닉네임은 비어있을 수 없습니다."),

  API_NOT_CONNECTED(HttpStatus.BAD_REQUEST, "API_001", "API를 불러오지 못했습니다.");
  
  // 실제 상태(필드)는 Enum이 가집니다.
    private final HttpStatus status;
    private final String code;
    private final String message;
}

