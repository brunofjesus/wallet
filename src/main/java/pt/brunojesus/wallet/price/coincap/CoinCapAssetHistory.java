package pt.brunojesus.wallet.price.coincap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoinCapAssetHistory {
    
    private Long timestamp;
    private List<Data> data;
    
    @lombok.Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        private String priceUsd;
        private Long time;
        private OffsetDateTime date;
    }
}