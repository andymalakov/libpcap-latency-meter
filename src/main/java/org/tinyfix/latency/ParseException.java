package org.tinyfix.latency;

final class ParseException extends RuntimeException {
    public ParseException(String message) {
        super(message);
    }
    public ParseException(String message, Throwable e) {
        super(message, e);
    }
}
