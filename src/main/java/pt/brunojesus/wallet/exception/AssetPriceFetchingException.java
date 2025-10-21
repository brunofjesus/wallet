package pt.brunojesus.wallet.exception;

import org.springframework.http.HttpStatus;

public class AssetPriceFetchingException extends BaseException {
    public AssetPriceFetchingException(String message) {
        this(message, null);
    }

    public AssetPriceFetchingException(String message, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, "PRICE_FETCH_ERROR", cause);
    }
}
