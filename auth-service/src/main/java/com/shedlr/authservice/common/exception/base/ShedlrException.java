package com.shedlr.authservice.common.exception.base;

import com.shedlr.authservice.common.exception.errorcode.ErrorCode;
import lombok.Getter;

/**
 * Base abstract exception for all custom business exceptions.
 * Carries an ErrorCode that defines the HTTP status and machine-readable code.
 */
@Getter
public abstract class ShedlrException extends RuntimeException {
    private final ErrorCode errorCode;

    protected ShedlrException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected ShedlrException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
