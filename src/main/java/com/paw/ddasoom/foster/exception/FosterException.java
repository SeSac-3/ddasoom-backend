package com.paw.ddasoom.foster.exception;

import com.paw.ddasoom.common.exception.BusinessException;

public class FosterException extends BusinessException {

  public FosterException(FosterErrorCode errorCode) {
    super(errorCode);
  }

}
