package pt.brunojesus.wallet.price;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class AssetPrice {
    private Instant timestamp;
    private BigDecimal price;
}
