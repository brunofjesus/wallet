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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class SimulationService {

    private final AssetPriceService assetPriceService;

    @Autowired
    public SimulationService(AssetPriceService assetPriceService) {
        this.assetPriceService = assetPriceService;
    }

    public SimulationResultDTO simulate(@Valid SimulationRequestDTO request) {
        List<AssetDTO> simulatedAssets = simulateAssets(request);

        BigDecimal totalValue = calculateTotalValue(simulatedAssets);
        AssetPerformance bestPerformance = findBestPerformance(simulatedAssets, request);
        AssetPerformance worstPerformance = findWorstPerformance(simulatedAssets, request);

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

    private Optional<AssetDTO> getSimulatedAsset(String symbol, BigDecimal quantity, Instant timestamp) {
        return assetPriceService.getHistoricalAssetPrice(
                        symbol,
                        timestamp.minus(1L, ChronoUnit.MINUTES),
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

    private BigDecimal calculateTotalValue(List<AssetDTO> assets) {
        return assets.stream()
                .map(AssetDTO::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private AssetPerformance findBestPerformance(List<AssetDTO> simulatedAssets, SimulationRequestDTO request) {
        return simulatedAssets.stream()
                .map(asset -> calculateAssetPerformance(asset, request))
                .max(Comparator.comparing(AssetPerformance::performance))
                .orElse(new AssetPerformance("", BigDecimal.ZERO));
    }

    private AssetPerformance findWorstPerformance(List<AssetDTO> simulatedAssets, SimulationRequestDTO request) {
        return simulatedAssets.stream()
                .map(asset -> calculateAssetPerformance(asset, request))
                .min(Comparator.comparing(AssetPerformance::performance))
                .orElse(new AssetPerformance("", BigDecimal.ZERO));
    }

    private AssetPerformance calculateAssetPerformance(AssetDTO simulatedAsset, SimulationRequestDTO request) {
        SimulationRequestDTO.SimulationAssetDTO originalAsset = findOriginalAsset(simulatedAsset.getSymbol(), request);

        BigDecimal originalInvestment = originalAsset.getValue();
        BigDecimal currentValue = simulatedAsset.getValue();
        BigDecimal percentDiff = currentValue.subtract(originalInvestment)
                .divide(originalInvestment, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        return new AssetPerformance(simulatedAsset.getSymbol(), percentDiff);
    }

    private SimulationRequestDTO.SimulationAssetDTO findOriginalAsset(String symbol, SimulationRequestDTO request) {
        // TODO: maybe we can receive a map of symbols -> original assets in order to improve performance
        return request.getAssets().stream()
                .filter(asset -> asset.getSymbol().equals(symbol))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Original asset not found for symbol: " + symbol));
    }

    private record AssetPerformance(String symbol, BigDecimal performance) {
    }
}
