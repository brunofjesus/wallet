package pt.brunojesus.wallet.price;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class AssetPrice {
    private Instant timestamp;
    private BigDecimal price;
}
