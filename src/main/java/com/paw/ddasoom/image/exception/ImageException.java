package com.paw.ddasoom.image.exception;

import com.paw.ddasoom.common.exception.BusinessException;

public class ImageException extends BusinessException {
    public ImageException(ImageErrorCode errorCode) {
        super(errorCode);
    }
}
