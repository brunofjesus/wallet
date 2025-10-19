package pt.brunojesus.wallet.exception;

public class AssetAlreadyExistsException extends RuntimeException {
    public AssetAlreadyExistsException(String message) {
        super(message);
    }
}
