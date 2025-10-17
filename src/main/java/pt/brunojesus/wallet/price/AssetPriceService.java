package pt.brunojesus.wallet.price;

import java.time.Instant;
import java.util.List;

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
    AssetPrice getAssetPriceBySymbol(String symbol) throws AssetPriceFetchingException;

    /**
     * Get the current price of an asset.
     * @param slug the asset slug to get the price (e.g. bitcoin, ethereum).
     * @return the AssetPrice result.
     *
     * @throws AssetPriceFetchingException if the asset price cannot be fetched.
     */
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
    List<AssetPrice> getHistoricalAssetPrice(String slug, Instant start, Instant end) throws AssetPriceFetchingException;
}
