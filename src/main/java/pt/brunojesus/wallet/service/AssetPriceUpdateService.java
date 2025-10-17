package pt.brunojesus.wallet.service;

import jakarta.persistence.PostLoad;
import pt.brunojesus.wallet.entity.Asset;
import pt.brunojesus.wallet.repository.AssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

@Service
public class AssetPriceUpdateService {

    private static final Logger log = LoggerFactory.getLogger(AssetPriceUpdateService.class);
    
    private final int threadCount;
    private final AssetRepository assetRepository;

    @Autowired
    public AssetPriceUpdateService(
            @Value("${app.tasks.asset.price-update.threads:3}") int threadCount,
            AssetRepository assetRepository) {
        this.threadCount = threadCount;
        this.assetRepository = assetRepository;
    }

    @Transactional
    @Scheduled(fixedRateString = "${app.tasks.asset.price-update.interval-ms:60000}")
    public void updateAllPrices() {
        try (ForkJoinPool pool = new ForkJoinPool(threadCount)) {
            pool.submit(() ->
                    assetRepository.streamAll()
                            .parallel()
                            .forEach(this::updateAssetPrice)
            ).get();
        } catch (InterruptedException e) {
            log.error("Price update interrupted", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("Price update failed", e);
            throw new RuntimeException(e);
        }
    }

    // TODO: query API
    private void updateAssetPrice(Asset asset) {
        try {
            log.debug("Updating price for asset: {}", asset.getId());
            final BigDecimal newPrice = new BigDecimal("100");
            asset.setUsdPrice(newPrice);
            asset.setLastUpdated(LocalDateTime.now());
            assetRepository.save(asset);
            log.debug("Successfully updated price for {}: ${}", asset.getId(), newPrice);
        } catch (Exception e) {
            log.error("Failed to update price for asset: {}", asset.getId(), e);
        }
    }
}