package com.paw.ddasoom.support.exception;

import com.paw.ddasoom.common.exception.BusinessException;

public class SupportException extends BusinessException {

  public SupportException(SupportErrorCode errorCdoe) {
    super(errorCdoe);
  }

}
