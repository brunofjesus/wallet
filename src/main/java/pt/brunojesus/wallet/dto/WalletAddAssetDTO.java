package pt.brunojesus.wallet.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletAddAssetDTO {

    /**
     * The symbol of the asset.
     */
    @NotBlank(message = "Symbol is required")
    @Size(min = 1, max = 10, message = "Symbol must be between 1 and 10 characters")
    private String symbol;

    /**
     * The price of the asset in USD.
     */
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be greater than or equal to zero")
    private BigDecimal price;

    /**
     * The number of acquired units.
     */
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", message = "Quantity must be greater than or equal to zero")
    private BigDecimal quantity;
}
