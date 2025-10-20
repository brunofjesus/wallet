package pt.brunojesus.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceDTO {
    private BigDecimal total;
    private List<AssetDTO> assets;
}
