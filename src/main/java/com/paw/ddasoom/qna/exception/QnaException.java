package com.paw.ddasoom.qna.exception;

import com.paw.ddasoom.common.exception.BusinessException;

public class QnaException extends BusinessException {

  public QnaException(QnaErrorCode errorCode) {
    super(errorCode);
  }
  
}
