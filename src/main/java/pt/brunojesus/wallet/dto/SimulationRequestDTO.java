package pt.brunojesus.wallet.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationRequestDTO {

    @NotNull(message = "Timestamp is required")
    @PastOrPresent(message = "Date must not be in the future")
    private Instant timestamp;

    @NotEmpty(message = "Assets are required")
    @Size(min = 1, max = 10, message = "You need at least one asset. Max 10 assets are allowed")
    private List<SimulationAssetDTO> assets;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimulationAssetDTO {
        @NotBlank(message = "Symbol is required")
        @Size(min = 1, max = 10, message = "Symbol must be between 1 and 10 characters")
        private String symbol;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", message = "Quantity must be greater than or equal to zero")
        private BigDecimal quantity;

        @NotNull(message = "Value is required")
        @DecimalMin(value = "0.0", message = "Value must be greater than or equal to zero")
        private BigDecimal value;
    }
}
