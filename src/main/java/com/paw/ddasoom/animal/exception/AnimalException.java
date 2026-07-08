package com.paw.ddasoom.animal.exception;

import com.paw.ddasoom.common.exception.BusinessException;

public class AnimalException extends BusinessException {

  public AnimalException(AnimalErrorCode errorCode){
    super(errorCode);
  }
}
