package pt.brunojesus.wallet.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public abstract class BaseException extends RuntimeException {
    private HttpStatus httpStatus;
    private String errorType;

    public BaseException(String message, HttpStatus httpStatus, String errorType) {
        this(message, httpStatus, errorType, null);
    }

    public BaseException(String message, HttpStatus httpStatus, String errorType, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorType = errorType;
    }
}
