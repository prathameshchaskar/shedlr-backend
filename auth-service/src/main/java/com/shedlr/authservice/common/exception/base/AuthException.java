package com.shedlr.authservice.common.exception.base;

import com.shedlr.authservice.common.exception.errorcode.ErrorCode;

/**
 * AuthException handles security and identity related failures.
 */
public class AuthException extends ShedlrException {
    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}
