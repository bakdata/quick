package com.bakdata.quick.common.exception;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.http.HttpStatus;

/**
 * An exception that can be thrown when working with a Quick mirror.
 */
public class MirrorException extends QuickException {
    private final HttpStatus status;

    public MirrorException(@Nullable final String message, final HttpStatus status) {
        super(message);
        this.status = status;
    }

    public MirrorException(final String message, final HttpStatus status, final Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    @Override
    protected HttpStatus getStatus() {
        return this.status;
    }


}
