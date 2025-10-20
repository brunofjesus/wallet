package pt.brunojesus.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResultDTO {
    private Instant timestamp;
    private BigDecimal total;
    private String bestAsset;
    private BigDecimal bestPerformance;
    private String worstAsset;
    private BigDecimal worstPerformance;
    private List<AssetDTO> assets;
}
