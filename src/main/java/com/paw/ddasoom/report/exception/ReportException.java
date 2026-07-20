package com.paw.ddasoom.report.exception;

import com.paw.ddasoom.common.exception.BusinessException;

public class ReportException extends BusinessException{
  public ReportException(ReportErrorCode errorCode) {
    super(errorCode);
  }
}
