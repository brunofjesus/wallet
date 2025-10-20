package pt.brunojesus.wallet.service;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.brunojesus.wallet.dto.AssetDTO;
import pt.brunojesus.wallet.dto.SimulationRequestDTO;
import pt.brunojesus.wallet.dto.SimulationResultDTO;
import pt.brunojesus.wallet.price.AssetPriceService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service for simulating portfolio performance at historical points in time.
 * Calculates asset values, total portfolio value, and performance metrics
 * based on historical price data from external sources.
 *
 * <p>This service allows users to see how their portfolio would have performed
 * if they had held specific assets at a given historical timestamp.</p>
 *
 * @author bruno
 * @see AssetPriceService
 */
@Service
public class SimulationService {

    private static final long HISTORICAL_PRICE_WINDOW_MINUTES = 5L;

    private final AssetPriceService assetPriceService;

    /**
     * Constructs a SimulationService with the required price service dependency.
     *
     * @param assetPriceService the service for fetching historical asset price data
     */
    @Autowired
    public SimulationService(AssetPriceService assetPriceService) {
        this.assetPriceService = assetPriceService;
    }

    /**
     * Simulates portfolio performance at a specific point in time.
     *
     * <p>This method calculates how a portfolio of assets would have performed
     * at the specified timestamp by fetching historical prices and computing
     * performance metrics including best and worst performing assets.</p>
     *
     * @param request the simulation parameters including assets, quantities, and target timestamp
     * @return simulation results containing total value, performance metrics, and individual asset data
     * @throws IllegalStateException if any original asset data is missing during performance calculation
     */
    public SimulationResultDTO simulate(@Valid SimulationRequestDTO request) {
        List<AssetDTO> simulatedAssets = simulateAssets(request);

        Map<String, SimulationRequestDTO.SimulationAssetDTO> originalAssetMap = new HashMap<>();
        request.getAssets().forEach(asset -> originalAssetMap.put(asset.getSymbol(), asset));

        BigDecimal totalValue = calculateTotalValue(simulatedAssets);
        AssetPerformance bestPerformance = findBestPerformance(simulatedAssets, originalAssetMap);
        AssetPerformance worstPerformance = findWorstPerformance(simulatedAssets, originalAssetMap);

        return new SimulationResultDTO(
                request.getTimestamp(),
                totalValue,
                bestPerformance.symbol(),
                bestPerformance.performance(),
                worstPerformance.symbol(),
                worstPerformance.performance(),
                simulatedAssets
        );
    }

    /**
     * Simulates individual assets by fetching their historical prices.
     *
     * <p>For each asset in the request, this method attempts to retrieve
     * historical price data and create simulated asset DTOs. Assets with
     * missing historical data are excluded from the results.</p>
     *
     * @param request the simulation request containing assets and timestamp
     * @return list of successfully simulated assets with historical price data
     */
    private List<AssetDTO> simulateAssets(SimulationRequestDTO request) {
        List<AssetDTO> simulatedAssets = new ArrayList<>();

        request.getAssets().forEach(requestAsset -> {
            getSimulatedAsset(
                    requestAsset.getSymbol(),
                    requestAsset.getQuantity(),
                    request.getTimestamp()
            ).ifPresent(simulatedAssets::add);
        });

        return simulatedAssets;
    }

    /**
     * Retrieves historical price data for a single asset and creates a simulated AssetDTO.
     *
     * <p>Fetches historical price data within a 5-minute window around the specified
     * timestamp and calculates the total value based on the provided quantity.</p>
     *
     * @param symbol    the asset symbol (e.g., "BTC", "ETH")
     * @param quantity  the quantity of the asset held
     * @param timestamp the target timestamp for historical price lookup
     * @return Optional containing the simulated AssetDTO if historical data is available, empty otherwise
     */
    private Optional<AssetDTO> getSimulatedAsset(String symbol, BigDecimal quantity, Instant timestamp) {
        return assetPriceService.getHistoricalAssetPrice(
                        symbol,
                        timestamp.minus(HISTORICAL_PRICE_WINDOW_MINUTES, ChronoUnit.MINUTES),
                        timestamp
                )
                .stream()
                .findFirst()
                .map(priceData -> AssetDTO.builder()
                        .symbol(symbol)
                        .quantity(quantity)
                        .price(priceData.getPrice())
                        .value(priceData.getPrice().multiply(quantity))
                        .build()
                );
    }

    /**
     * Calculates the total portfolio value by summing all individual asset values.
     *
     * @param assets list of simulated assets with calculated values
     * @return the total value of all assets combined
     */
    private BigDecimal calculateTotalValue(List<AssetDTO> assets) {
        return assets.stream()
                .map(AssetDTO::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Finds the asset with the highest performance percentage.
     *
     * @param simulatedAssets        list of simulated assets with current values
     * @param originalAssetsBySymbol map of original asset data indexed by symbol
     * @return the asset performance data for the best performing asset, or empty performance if no assets
     */
    private AssetPerformance findBestPerformance(
            List<AssetDTO> simulatedAssets,
            Map<String, SimulationRequestDTO.SimulationAssetDTO> originalAssetsBySymbol
    ) {
        return simulatedAssets.stream()
                .map(asset -> calculateAssetPerformance(asset, originalAssetsBySymbol))
                .max(Comparator.comparing(AssetPerformance::performance))
                .orElse(new AssetPerformance("", BigDecimal.ZERO));
    }

    /**
     * Finds the asset with the lowest performance percentage.
     *
     * @param simulatedAssets        list of simulated assets with current values
     * @param originalAssetsBySymbol map of original asset data indexed by symbol
     * @return the asset performance data for the worst performing asset, or empty performance if no assets
     */
    private AssetPerformance findWorstPerformance(
            List<AssetDTO> simulatedAssets,
            Map<String, SimulationRequestDTO.SimulationAssetDTO> originalAssetsBySymbol
    ) {
        return simulatedAssets.stream()
                .map(asset -> calculateAssetPerformance(asset, originalAssetsBySymbol))
                .min(Comparator.comparing(AssetPerformance::performance))
                .orElse(new AssetPerformance("", BigDecimal.ZERO));
    }

    /**
     * Calculates the percentage performance of an asset compared to its original investment.
     *
     * <p>Performance is calculated as: ((current_value - original_investment) / original_investment) * 100</p>
     *
     * @param simulatedAsset         the asset with current simulated value
     * @param originalAssetsBySymbol map containing original asset data indexed by symbol
     * @return AssetPerformance containing symbol and percentage performance
     * @throws IllegalStateException if the original asset data is not found for the given symbol
     */
    private AssetPerformance calculateAssetPerformance(
            AssetDTO simulatedAsset,
            Map<String, SimulationRequestDTO.SimulationAssetDTO> originalAssetsBySymbol
    ) {
        SimulationRequestDTO.SimulationAssetDTO originalAsset = originalAssetsBySymbol.get(simulatedAsset.getSymbol());
        if (originalAsset == null) {
            throw new IllegalStateException("Original asset not found for symbol: " + simulatedAsset.getSymbol());
        }

        BigDecimal originalInvestment = originalAsset.getValue();
        BigDecimal currentValue = simulatedAsset.getValue();
        BigDecimal absoluteDiff = currentValue.subtract(originalInvestment);
        BigDecimal percentDiff = absoluteDiff
                .divide(originalInvestment, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        return new AssetPerformance(simulatedAsset.getSymbol(), percentDiff);
    }

    /**
     * Record representing asset performance data.
     *
     * @param symbol      the asset symbol
     * @param performance the percentage performance (positive for gains, negative for losses)
     */
    private record AssetPerformance(String symbol, BigDecimal performance) {
    }
}
