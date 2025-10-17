package pt.brunojesus.wallet.price.coincap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoinCapAsset {
    private Long timestamp;
    private CoinCapAssetData data;
}