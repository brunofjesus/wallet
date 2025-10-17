package pt.brunojesus.wallet.price.coincap;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@lombok.Data
@NoArgsConstructor
@AllArgsConstructor
public class CoinCapAssetData {
    private String id;
    private String rank;
    private String symbol;
    private String name;
    private String supply;
    private String maxSupply;
    private String marketCapUsd;
    @JsonProperty("volumeUsd24Hr")
    private String volumeUsd24Hr;
    private String priceUsd;
    @JsonProperty("changePercent24Hr")
    private String changePercent24Hr;
    @JsonProperty("vwap24Hr")
    private String vwap24Hr;
    private String explorer;
    private Map<String, List<String>> tokens;
}

