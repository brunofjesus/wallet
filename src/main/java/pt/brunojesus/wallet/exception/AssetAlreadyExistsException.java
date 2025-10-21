package pt.brunojesus.wallet.exception;

import org.springframework.http.HttpStatus;

public class AssetAlreadyExistsException extends BaseException {
    public AssetAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT, "ASSET_ALREADY_EXISTS");
    }
}
