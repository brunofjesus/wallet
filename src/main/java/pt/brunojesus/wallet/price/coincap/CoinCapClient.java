package pt.brunojesus.wallet.price.coincap;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "coincap", url = "https://rest.coincap.io/v3/")
public interface CoinCapClient {

    /**
     * Search assets by slug or symbol.
     *
     * @param authorization The token to be used for authentication.
     * @param slugOrSymbol  The asset slug (bitcoin) or symbol (BTC)
     * @param idsCsv        Comma-separated list of asset ids (e.g. bitcoin,ethereum)
     * @param limit         The maximum number of results to return (default is 100).
     * @param offset        Number of results to skip (default is 0).
     * @return The CoinCapAssets result
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/assets",
            produces = "application/json"
    )
    CoinCapAssets searchAssets(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "search", required = false) String slugOrSymbol,
            @RequestParam(value = "ids", required = false) String idsCsv,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset
    );

    /**
     * Get the CoinCap asset by slug.
     *
     * @param authorization The token to be used for authentication.
     * @param slug          The asset slug (e.g. bitcoin, ethereum)
     * @return The CoinCapAsset result
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/assets/{slug}",
            produces = "application/json"
    )
    CoinCapAsset getAsset(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("slug") String slug
    );

    /**
     * Get the CoinCap asset history by slug.
     *
     * @param authorization The token to be used for authentication.
     * @param slug          The asset slug (e.g. bitcoin, ethereum)
     * @param interval      The interval (e.g. m1, m5, m15, m30, h1, h2, h6, h12, d1)
     * @param start         UNIX time in milliseconds. Omitting will return the most recent asset history.
     * @param end           The end timestamp for the historical data.
     * @return The CoinCapAssetHistory result
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/assets/{slug}/history",
            produces = "application/json"
    )
    CoinCapAssetHistory getAssetHistory(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("slug") String slug,
            @RequestParam("interval") String interval,
            @RequestParam(value = "start", required = false) Long start,
            @RequestParam(value = "end", required = false) Long end
    );

    /**
     * Get the CoinCap asset price by symbol.
     *
     * @param authorization The token to be used for authentication.
     * @param symbol        Single symbol (e.g., "BTC") or comma-separated symbols (e.g., "BTC,ETH"). Maximum 100 symbols.
     * @return CoinCapPriceBySymbol with list of price strings in the same order as the requested symbols.
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/price/bysymbol/{symbol}",
            produces = "application/json"
    )
    CoinCapPriceBySymbol getPriceBySymbol(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("symbol") String symbol
    );
}
