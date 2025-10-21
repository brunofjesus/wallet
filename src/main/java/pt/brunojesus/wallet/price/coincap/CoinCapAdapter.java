package pt.brunojesus.wallet.price.coincap;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import pt.brunojesus.wallet.price.AssetPrice;
import pt.brunojesus.wallet.exception.AssetPriceFetchingException;
import pt.brunojesus.wallet.price.AssetPriceService;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter for retrieving asset pricing data from the CoinCap API.
 * This class interacts with the CoinCapClient to fetch real-time and historical
 * asset pricing information based on asset symbols or slugs.
 * <p>
 * Implements the AssetPriceService interface.
 * <p>
 * <b>Warning: </b> There's no rate limiting implemented in this adapter.
 * @author bruno
 */
@Component
@Validated
@Slf4j
public class CoinCapAdapter implements AssetPriceService {

    // Poor man's cache to avoid repeated API calls
    private final Map<String, String> symbolToSlugCache = new ConcurrentHashMap<>();

    private final String token;
    private final CoinCapClient coinCapClient;

    @Autowired
    public CoinCapAdapter(
            @Value("${app.price.coincap.token}") String token,
            CoinCapClient coinCapClient
    ) {
        this.token = ObjectUtils.isEmpty(token) ? null : "Bearer " + token;
        this.coinCapClient = coinCapClient;
    }

    /**
     * @inheritDoc
     */
    @Override
    @NonNull
    public AssetPrice getAssetPrice(String symbol) throws AssetPriceFetchingException {
        if (ObjectUtils.isEmpty(symbol)) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }

        return getAssetPrices(List.of(symbol)).get(symbol);
    }

    /**
     * @inheritDoc
     */
    @Override
    @NonNull
    public Map<String,AssetPrice> getAssetPrices(List<String> symbols) throws AssetPriceFetchingException {
        if (ObjectUtils.isEmpty(symbols)) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }

        if (symbols.size() > 100) {
            throw new IllegalArgumentException("Maximum number of symbols to fetch is 100");
        }

        String symbolString = String.join(",", symbols);

        final CoinCapPriceBySymbol coinCapAssets;
        try {
            coinCapAssets = coinCapClient.getPriceBySymbol(this.token, symbolString);
        } catch (FeignException e) {
            throw new AssetPriceFetchingException("Failed to fetch asset price for symbol(s): " + symbolString, e);
        } catch (Exception e) {
            throw new AssetPriceFetchingException(
                    "Unexpected error while fetching asset price for symbol(s): " + symbolString, e
            );
        }

        if (coinCapAssets == null || coinCapAssets.getData() == null) {
            throw new AssetPriceFetchingException(
                    "Failed to fetch asset price for symbol(s): " + symbolString + ". CoinCap returned an empty data response"
            );
        }

        if (coinCapAssets.getData().size() != symbols.size()) {
            throw new AssetPriceFetchingException(
                    "Failed to fetch asset price for symbol(s): " + symbolString + ". CoinCap returned an invalid number of data items"
            );
        }

        final Instant timestamp = Instant.ofEpochMilli(coinCapAssets.getTimestamp());
        return buildSymbolToPriceMap(symbols, coinCapAssets, timestamp);
    }

    /**
     * Get the historical price of an asset.
     *
     * @param symbol the asset symbol to get the price (e.g. BTC, ETH).
     * @param start the start timestamp for the historical data.
     * @param end the end timestamp for the historical data.
     * @return the list of AssetPrice results.
     * @throws AssetPriceFetchingException if the asset price cannot be fetched.
     */
    @Override
    @NonNull
    public List<AssetPrice> getHistoricalAssetPrice(String symbol, Instant start, Instant end) throws AssetPriceFetchingException {
        if (ObjectUtils.isEmpty(symbol)) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }

        if ((start != null && end == null) || (start == null && end != null)) {
            throw new IllegalArgumentException("Both start and end timestamps or none must be provided");
        }

        if (start != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Start timestamp must be before end timestamp");
        }

        String slug = getSlugFromSymbol(symbol);
        return getHistoricalAssetPriceBySlug(slug, start, end, "d1");
    }


    /**
     * Get historical asset prices by slug
     */
    @NonNull
    private List<AssetPrice> getHistoricalAssetPriceBySlug(String slug, Instant start, Instant end, String interval) throws AssetPriceFetchingException {
        if (ObjectUtils.isEmpty(slug)) {
            throw new IllegalArgumentException("Slug cannot be null or empty");
        }

        Long startTimestamp = start == null ? null : start.toEpochMilli();
        Long endTimestamp = end == null ? null : end.toEpochMilli();

        final CoinCapAssetHistory history;
        try {
            history = coinCapClient.getAssetHistory(
                    this.token, slug, interval, startTimestamp, endTimestamp
            );
        } catch (FeignException e) {
            log.error("Failed to fetch historical asset prices for slug: {}", slug, e);
            throw new AssetPriceFetchingException("Failed to fetch historical asset prices for slug: " + slug, e);
        } catch (Exception e) {
            log.error("Unexpected error while fetching historical asset prices for slug: {}", slug, e);
            throw new AssetPriceFetchingException("Unexpected error while fetching historical asset prices for slug: " + slug, e);
        }

        if (history == null || history.getData() == null || history.getData().isEmpty()) {
            return List.of();
        }

        try {
            return history.getData().stream()
                    .map(data -> AssetPrice.builder()
                            .timestamp(Instant.ofEpochMilli(data.getTime()))
                            .price(new BigDecimal(data.getPriceUsd()))
                            .build())
                    .toList();
        } catch (NumberFormatException e) {
            log.error("Invalid historical price data received for slug: {}", slug, e);
            throw new AssetPriceFetchingException("Invalid historical price data received for slug: " + slug, e);
        } catch (Exception e) {
            log.error("Failed to process historical asset price data for slug: {}", slug, e);
            throw new AssetPriceFetchingException("Failed to process historical asset price data for slug: " + slug, e);
        }
    }

    @NonNull
    private static Map<String, AssetPrice> buildSymbolToPriceMap(List<String> symbols, CoinCapPriceBySymbol coinCapAssets, Instant timestamp) throws AssetPriceFetchingException {
        Map<String, AssetPrice> assetPriceMap = new HashMap<>();
        for (int i = 0; i < coinCapAssets.getData().size(); i++) {
            try {
                final AssetPrice assetPrice = new AssetPrice(
                        timestamp,
                        new BigDecimal(coinCapAssets.getData().get(i))
                );
                assetPriceMap.put(symbols.get(i), assetPrice);
            } catch (NumberFormatException e) {
                throw new AssetPriceFetchingException("Invalid price data received for symbol: " + symbols.get(i), e);
            } catch (Exception e) {
                throw new AssetPriceFetchingException("Failed to process asset price data for symbol: " + symbols.get(i), e);
            }
        }
        return assetPriceMap;
    }

    /**
     * Get the slug for an asset symbol, with caching to avoid repeated API calls.
     *
     * @param symbol the asset symbol
     * @return the asset slug
     * @throws AssetPriceFetchingException if the asset cannot be found
     */
    @NonNull
    private String getSlugFromSymbol(String symbol) throws AssetPriceFetchingException {
        String normalizedSymbol = symbol.toUpperCase();

        // Check cache first
        if (symbolToSlugCache.containsKey(normalizedSymbol)) {
            return symbolToSlugCache.get(normalizedSymbol);
        }

        // Fetch from API
        try {
            CoinCapAssets assets = coinCapClient.searchAssets(token, symbol, null, 8, 0);
            if (assets.getData().isEmpty()) {
                throw new AssetPriceFetchingException("No asset found for symbol: " + symbol);
            }

            // Find an exact symbol match (case-insensitive)
            CoinCapAssetData assetData = assets.getData().stream()
                    .filter(asset -> normalizedSymbol.equals(asset.getSymbol().toUpperCase()))
                    .findFirst()
                    .orElseThrow(() -> new AssetPriceFetchingException("No asset found for symbol: " + symbol));

            String slug = assetData.getId();

            // Cache the result
            symbolToSlugCache.put(normalizedSymbol, slug);

            return slug;
        } catch (FeignException e) {
            throw new AssetPriceFetchingException("Failed to find asset for symbol: " + symbol, e);
        } catch (Exception e) {
            throw new AssetPriceFetchingException("Unexpected error while searching for asset: " + symbol, e);
        }
    }

}
