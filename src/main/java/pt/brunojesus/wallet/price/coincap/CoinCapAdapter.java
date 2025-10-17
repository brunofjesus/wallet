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
import java.util.List;

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

        final CoinCapAssets coinCapAssets;
        try {
            coinCapAssets = coinCapClient.searchAssets(this.token, symbol, null, 1, 0);
        } catch (FeignException e) {
            throw new AssetPriceFetchingException("Failed to fetch asset price for symbol: " + symbol, e);
        } catch (Exception e) {
            throw new AssetPriceFetchingException("Unexpected error while fetching asset price for symbol: " + symbol, e);
        }

        if (coinCapAssets == null || coinCapAssets.getData() == null) {
            throw new AssetPriceFetchingException("Failed to fetch asset price for symbol: " + symbol + ". CoinCap returned an empty data response");
        }

        try {
            return coinCapAssets.getData().stream()
                    .filter(data -> symbol.equalsIgnoreCase(data.getSymbol()))
                    .findAny()
                    .map(data -> AssetPrice.builder()
                            .timestamp(Instant.ofEpochMilli(coinCapAssets.getTimestamp()))
                            .price(new BigDecimal(data.getPriceUsd()))
                            .build()
                    ).orElse(null);
        } catch (NumberFormatException e) {
            throw new AssetPriceFetchingException("Invalid price data received for symbol: " + symbol, e);
        } catch (Exception e) {
            throw new AssetPriceFetchingException("Failed to process asset price data for symbol: " + symbol, e);
        }
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
