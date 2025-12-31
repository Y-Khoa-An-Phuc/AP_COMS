package com.src.ap.exception;

import lombok.Getter;

@Getter
public class PasswordChangeRequiredException extends RuntimeException {
    private final boolean mustChangePassword;
    private final boolean firstLogin;

    public PasswordChangeRequiredException(String message, boolean mustChangePassword, boolean firstLogin) {
        super(message);
        this.mustChangePassword = mustChangePassword;
        this.firstLogin = firstLogin;
    }

    public PasswordChangeRequiredException(String message) {
        this(message, true, true);
    }
}