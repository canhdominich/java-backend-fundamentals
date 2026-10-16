package dev.canhdominich.jdbc.dao;

public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public static final class DuplicateKeyException extends DataAccessException {

        public DuplicateKeyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
