package pt.brunojesus.wallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDTO {
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal value;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Instant timestamp;
}
