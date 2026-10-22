package com.turbo.mobile.data.model;

public abstract class Result<T> {

    private Result() {
    }

    public static final class Success<T> extends Result<T> {
        public final T data;

        public Success(T data) {
            this.data = data;
        }
    }

    public static final class Error<T> extends Result<T> {
        public final RepoError code;
        public final String message;

        public Error(RepoError code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    public static <T> Result<T> success(T data) {
        return new Success<>(data);
    }

    public static <T> Result<T> error(RepoError code, String message) {
        return new Error<>(code, message);
    }
}
