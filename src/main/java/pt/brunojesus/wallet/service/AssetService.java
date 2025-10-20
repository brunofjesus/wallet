package pt.brunojesus.wallet.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.brunojesus.wallet.entity.Asset;
import pt.brunojesus.wallet.price.AssetPrice;
import pt.brunojesus.wallet.price.AssetPriceService;
import pt.brunojesus.wallet.repository.AssetRepository;

import java.util.List;
import java.util.stream.Stream;

/**
 * Service class responsible for managing assets and their price updates.
 *
 * <p>This service provides operations for retrieving assets and updating their USD prices
 * from external price sources. It acts as a business layer between the repository and
 * the asset price fetching service.</p>
 *
 * @author bruno
 */
@Service
@Slf4j
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetPriceService assetPriceService;

    /**
     * Constructs an AssetService with required dependencies.
     *
     * @param assetRepository   the repository for accessing asset data on the database
     * @param assetPriceService the service for fetching asset prices from external sources
     */
    @Autowired
    public AssetService(AssetRepository assetRepository, AssetPriceService assetPriceService) {
        this.assetRepository = assetRepository;
        this.assetPriceService = assetPriceService;
    }

    /**
     * Streams all assets from the database.
     *
     * @return a stream of assets
     */
    @Transactional(readOnly = true)
    public Stream<Asset> streamAll() {
        return assetRepository.streamAll();
    }

    @Transactional(readOnly = true)
    public List<Asset> findByIds(List<String> ids) {
        return assetRepository.findAllById(ids);
    }

    /**
     * Gets an asset from the database or creates it if it doesn't exist.
     *
     * <p>This method first attempts to find an asset by its symbol in the database.
     * If the asset is not found, it fetches the current price from an external source
     * and creates a new asset record with that price information.</p>
     *
     * @param symbol the asset symbol to get or create
     * @return the existing or newly created asset
     */
    @Transactional
    public Asset findOrCreateAsset(String symbol) {
        return assetRepository.findById(symbol).orElseGet(() -> {
                    AssetPrice assetPrice = assetPriceService.getAssetPrice(symbol);
                    return assetRepository.save(
                            Asset.builder()
                                    .id(symbol)
                                    .usdPrice(assetPrice.getPrice())
                                    .createdAt(assetPrice.getTimestamp())
                                    .updatedAt(assetPrice.getTimestamp())
                                    .build()
                    );
                }
        );
    }

    /**
     * Refreshes the USD price of the specified asset.
     *
     * <p>This method fetches the current price for the given asset from an external
     * price service and updates the asset's USD price in the database. The operation
     * is logged for debugging purposes and any exceptions are caught and logged as
     * errors without propagating them.</p>
     *
     * @param asset the asset whose price should be refreshed
     */
    @Transactional
    public void refreshAssetPrice(Asset asset) {
        try {
            log.debug("Updating price for asset: {}", asset.getId());
            AssetPrice assetPrice = this.assetPriceService.getAssetPrice(asset.getId());
            asset.setUsdPrice(assetPrice.getPrice());
            assetRepository.save(asset);
            log.debug("Successfully updated price for {}: ${}", asset.getId(), assetPrice.getPrice());
        } catch (Exception e) {
            log.error("Failed to update price for asset: {}", asset.getId(), e);
        }
    }
}
