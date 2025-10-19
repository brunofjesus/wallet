package pt.brunojesus.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletInfoDTO {

    private UUID id;
    private BalanceDTO original;
    private BalanceDTO current;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceDTO {
        private BigDecimal total;
        private List<AssetDTO> assets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetDTO {
        private String symbol;
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal value;
        private Instant timestamp;
    }
}
