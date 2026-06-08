package org.codrshi.error;

/**
 * Application-level exception carrying a client-friendly message.
 * <p>
 * Only the {@code message} (and optional {@code hint}) of a SemseaException
 * are ever shown to the user. The original cause, response body, stack trace,
 * etc. must be logged via log4j before/while throwing this exception.
 * <p>
 * Use the {@link #SemseaException(String, String)} two-argument constructor
 * when the failure is a user-facing validation problem (e.g. "no workspace
 * attached") for which a remediation hint is more useful than the generic
 * "see log file" pointer.
 */
public class SemseaException extends RuntimeException {

    private final String hint;

    public SemseaException(String message) {
        this(message, null, null);
    }

    public SemseaException(String message, Throwable cause) {
        this(message, null, cause);
    }

    public SemseaException(String message, String hint) {
        this(message, hint, null);
    }

    public SemseaException(String message, String hint, Throwable cause) {
        super(message, cause);
        this.hint = hint;
    }

    public String getHint() {
        return hint;
    }
}
