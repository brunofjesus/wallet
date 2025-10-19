package pt.brunojesus.wallet.price;

import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service to get asset prices from external sources.
 *
 * @author bruno
 */
public interface AssetPriceService {

    /**
     * Get the current price of an asset.
     * @param symbol the asset symbol to get the price (e.g. BTC, ETH, USDC, ...)
     * @return the AssetPrice result.
     *
     * @throws AssetPriceFetchingException if the asset price cannot be fetched.
     */
    @NonNull
    AssetPrice getAssetPriceBySymbol(String symbol) throws AssetPriceFetchingException;

    /**
     * Retrieves current prices for multiple assets by their symbols in a single API call.
     * This method is more efficient than calling {@link #getAssetPriceBySymbol(String)}
     * multiple times as it batches the requests.
     *
     * @param symbols list of asset symbols to get prices for (e.g. ["BTC", "ETH", "USDC"]).
     *                Must not be null or empty. Maximum 100 symbols per request.
     * @return a map where keys are the asset symbols and values are their corresponding AssetPrice objects.
     *         The map preserves the order and count of the input symbols.
     *
     * @throws IllegalArgumentException if symbols list is null, empty or has more than 100 elements.
     * @throws AssetPriceFetchingException if the asset prices cannot be fetched, including:
     *         - Network or API communication errors
     *         - Invalid response format from CoinCap API
     *         - Mismatch between requested and returned symbol counts
     *         - Invalid price data that cannot be parsed
     */
    @NonNull
    Map<String,AssetPrice> getAssetPriceBySymbols(List<String> symbols) throws AssetPriceFetchingException;

    /**
     * Get the current price of an asset.
     * @param slug the asset slug to get the price (e.g. bitcoin, ethereum).
     * @return the AssetPrice result.
     *
     * @throws AssetPriceFetchingException if the asset price cannot be fetched.
     */
    @NonNull
    AssetPrice getAssetPriceBySlug(String slug) throws AssetPriceFetchingException;

    /**
     * Get the historical price of an asset.
     *
     * @param slug the asset slug to get the price (e.g. bitcoin, ethereum).
     * @param start the start timestamp for the historical data.
     * @param end   the end timestamp for the historical data.
     * @return the AssetPrice result.
     *
     * @throws AssetPriceFetchingException if the asset price cannot be fetched.
     */
    @NonNull
    List<AssetPrice> getHistoricalAssetPrice(String slug, Instant start, Instant end) throws AssetPriceFetchingException;
}
