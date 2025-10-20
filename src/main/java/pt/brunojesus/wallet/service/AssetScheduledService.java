package pt.brunojesus.wallet.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.brunojesus.wallet.entity.Asset;

import java.util.concurrent.*;

/**
 * Service responsible for scheduled asset-related operations.
 *
 * <p>This service handles periodic tasks related to asset management,
 * particularly the automatic updating of asset prices using a configurable
 * thread pool to ensure efficient concurrent processing.</p>
 *
 * <p>The service uses virtual threads for better resource utilization and a
 * semaphore to control the maximum number of concurrent price update operations.</p>
 *
 * @author bruno
 */
@Slf4j
@Service
public class AssetScheduledService {

    /**
     * The maximum number of concurrent threads allowed for asset price updates.
     * Controlled by a semaphore to prevent overwhelming external APIs.
     */
    private final int threadCount;

    private final AssetService assetService;

    /**
     * Constructs a new AssetScheduledService with the specified configuration.
     *
     * @param threadCount the maximum number of concurrent threads for price updates,
     *                   configurable via {@code app.tasks.asset.price-update.threads} property
     *                   (defaults to 3 if not specified)
     * @param assetService the asset service to use for price update operations
     */
    @Autowired
    public AssetScheduledService(
            @Value("${app.tasks.asset.price-update.threads:3}") int threadCount,
            AssetService assetService) {
        this.threadCount = threadCount;
        this.assetService = assetService;
    }

    /**
     * Scheduled method that updates prices for all assets in the system.
     *
     * <p>This method runs at fixed intervals as configured by the
     * {@code app.tasks.asset.price-update.interval-ms} property (defaults to 60 seconds).
     * It processes all assets concurrently using virtual threads while limiting
     * the number of simultaneous operations through a semaphore.</p>
     *
     * <p>The method performs the following operations:</p>
     * <ul>
     *   <li>Creates a virtual thread executor for efficient concurrent processing</li>
     *   <li>Uses a semaphore to limit concurrent operations to the configured thread count</li>
     *   <li>Processes all assets in parallel, updating their prices</li>
     *   <li>Handles interruptions and exceptions gracefully</li>
     *   <li>Waits for all tasks to complete before finishing</li>
     * </ul>
     *
     * <p><strong>Error Handling:</strong></p>
     * <ul>
     *   <li>Individual asset update failures are logged but don't stop the entire process</li>
     *   <li>Thread interruptions are handled properly and logged</li>
     *   <li>Overall process failures are logged with full exception details</li>
     * </ul>
     *
     * @see AssetService#streamAll()
     * @see AssetService#refreshAssetPrice(Asset)
     */
    @Transactional
    //@Scheduled(fixedRateString = "${app.tasks.asset.price-update.interval-ms:60000}")
    public void updateAllPrices() {
        log.info("Starting asset price update");
        Semaphore semaphore = new Semaphore(threadCount);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                    assetService.streamAll()
                            .map(asset -> CompletableFuture.runAsync(() -> {
                                try {
                                    semaphore.acquire();
                                    assetService.refreshAssetPrice(asset);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    log.warn("Asset price update interrupted for {}", asset.getId());
                                } catch (Exception e) {
                                    log.error("Failed to update price for asset: {}", asset.getId(), e);
                                } finally {
                                    semaphore.release();
                                }
                            }, executor))
                            .toArray(CompletableFuture[]::new)
            );

            allTasks.join(); // Wait for all tasks to complete
            log.info("Asset price update completed");
        } catch (Exception e) {
            log.error("Asset price update failed", e);
        }
    }
}