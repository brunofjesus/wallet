package pt.brunojesus.wallet.price;

public class AssetPriceFetchingException extends Exception {
    public AssetPriceFetchingException(String message) {
        super(message);
    }

    public AssetPriceFetchingException(String message, Throwable cause) {
        super(message, cause);
    }
}
