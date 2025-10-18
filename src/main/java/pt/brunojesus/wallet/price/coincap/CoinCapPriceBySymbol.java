package pt.brunojesus.wallet.price.coincap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoinCapPriceBySymbol {
    private Long timestamp;
    private List<String> data;
}
