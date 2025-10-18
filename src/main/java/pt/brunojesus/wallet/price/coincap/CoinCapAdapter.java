package pt.brunojesus.wallet.price.coincap;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import pt.brunojesus.wallet.price.AssetPrice;
import pt.brunojesus.wallet.price.AssetPriceFetchingException;
import pt.brunojesus.wallet.price.AssetPriceService;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for retrieving asset pricing data from the CoinCap API.
 * This class interacts with the CoinCapClient to fetch real-time and historical
 * asset pricing information based on asset symbols or slugs.
 * Implements the AssetPriceService interface.
 *
 * <b>Warning: </b> There's no rate limiting implemented in this adapter.
 * @author bruno
 */
@Component
@Validated
public class CoinCapAdapter implements AssetPriceService {

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
    public AssetPrice getAssetPriceBySymbol(String symbol) throws AssetPriceFetchingException {
        if (ObjectUtils.isEmpty(symbol)) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }

        return getAssetPriceBySymbols(List.of(symbol)).get(symbol);
    }

    /**
     * @inheritDoc
     */
    public Map<String,AssetPrice> getAssetPriceBySymbols(List<String> symbols) throws AssetPriceFetchingException {
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
     * @inheritDoc
     */
    @Override
    public AssetPrice getAssetPriceBySlug(String slug) throws AssetPriceFetchingException {
        if (ObjectUtils.isEmpty(slug)) {
            throw new IllegalArgumentException("Slug cannot be null or empty");
        }

        final CoinCapAsset coinCapAsset;
        try {
            coinCapAsset = coinCapClient.getAsset(this.token, slug);
        } catch (FeignException e) {
            throw new AssetPriceFetchingException("Failed to fetch asset price for slug: " + slug, e);
        } catch (Exception e) {
            throw new AssetPriceFetchingException("Unexpected error while fetching asset price for slug: " + slug, e);
        }

        if (coinCapAsset == null || coinCapAsset.getData() == null) {
            throw new AssetPriceFetchingException("Failed to fetch asset price for slug: " + slug + ". CoinCap returned an empty data response");
        }

        try {
            return AssetPrice.builder()
                    .timestamp(Instant.ofEpochMilli(coinCapAsset.getTimestamp()))
                    .price(new BigDecimal(coinCapAsset.getData().getPriceUsd()))
                    .build();
        } catch (NumberFormatException e) {
            throw new AssetPriceFetchingException("Invalid price data received for slug: " + slug, e);
        } catch (Exception e) {
            throw new AssetPriceFetchingException("Failed to process asset price data for slug: " + slug, e);
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public List<AssetPrice> getHistoricalAssetPrice(String slug, Instant start, Instant end) throws AssetPriceFetchingException {
        if (ObjectUtils.isEmpty(slug)) {
            throw new IllegalArgumentException("Slug cannot be null or empty");
        }

        Long startTimestamp = start == null ? null : start.toEpochMilli();
        Long endTimestamp = end == null ? null : end.toEpochMilli();

        final CoinCapAssetHistory history;
        try {
            history = coinCapClient.getAssetHistory(
                    this.token, slug, "d1", startTimestamp, endTimestamp
            );
        } catch (FeignException e) {
            throw new AssetPriceFetchingException("Failed to fetch historical asset prices for slug: " + slug, e);
        } catch (Exception e) {
            throw new AssetPriceFetchingException("Unexpected error while fetching historical asset prices for slug: " + slug, e);
        }

        if (history == null || history.getData() == null || history.getData().isEmpty()) {
            return List.of();
        }

        try {
            return history.getData().stream().map(data ->
                    AssetPrice.builder()
                            .timestamp(Instant.ofEpochMilli(data.getTime()))
                            .price(new BigDecimal(data.getPriceUsd()))
                            .build()
            ).toList();
        } catch (NumberFormatException e) {
            throw new AssetPriceFetchingException("Invalid historical price data received for slug: " + slug, e);
        } catch (Exception e) {
            throw new AssetPriceFetchingException("Failed to process historical asset price data for slug: " + slug, e);
        }
    }
}
