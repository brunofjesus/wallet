package pt.brunojesus.wallet.exception;

import org.springframework.http.HttpStatus;

public class AssetNotFoundException extends BaseException {
    public AssetNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "ASSET_NOT_FOUND");
    }
}
