package pt.brunojesus.wallet.service;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.brunojesus.wallet.dto.AssetDTO;
import pt.brunojesus.wallet.dto.BalanceDTO;
import pt.brunojesus.wallet.dto.WalletAddAssetRequestDTO;
import pt.brunojesus.wallet.dto.WalletInfoDTO;
import pt.brunojesus.wallet.entity.Asset;
import pt.brunojesus.wallet.entity.User;
import pt.brunojesus.wallet.entity.UserAsset;
import pt.brunojesus.wallet.exception.AssetAlreadyExistsException;
import pt.brunojesus.wallet.exception.AssetNotFoundException;
import pt.brunojesus.wallet.price.AssetPrice;
import pt.brunojesus.wallet.price.AssetPriceService;
import pt.brunojesus.wallet.repository.AssetRepository;
import pt.brunojesus.wallet.repository.UserAssetRepository;

import java.math.BigDecimal;
import java.util.*;


@Service
public class WalletService {

    private final AssetRepository assetRepository;
    private final UserAssetRepository userAssetRepository;
    private final AssetPriceService assetPriceService;
    private final UserService userService;

    @Autowired
    public WalletService(
            AssetRepository assetRepository,
            UserAssetRepository userAssetRepository,
            AssetPriceService assetPriceService,
            UserService userService
    ) {
        this.assetRepository = assetRepository;
        this.userAssetRepository = userAssetRepository;
        this.assetPriceService = assetPriceService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public WalletInfoDTO info() {
        User currentUser = userService.getCurrentUser();

        List<UserAsset> userAssetsList = userAssetRepository.findByIdUserId(currentUser.getId());

        BalanceDTO originalBalance = calculateOriginalBalance(userAssetsList);
        BalanceDTO currentBalance = calculateCurrentBalance(userAssetsList);

        return new WalletInfoDTO(currentUser.getId(), originalBalance, currentBalance);
    }

    private BalanceDTO calculateOriginalBalance(List<UserAsset> userAssetList) {
        BigDecimal totalValue = BigDecimal.ZERO;
        List<AssetDTO> assetDtoList = new ArrayList<>(userAssetList.size());

        for (UserAsset userAsset : userAssetList) {
            BigDecimal assetValue = userAsset.getQuantity().multiply(userAsset.getPrice());
            totalValue = totalValue.add(assetValue);

            assetDtoList.add(AssetDTO.builder()
                    .symbol(userAsset.getId().getAssetId())
                    .quantity(userAsset.getQuantity())
                    .price(userAsset.getPrice())
                    .value(assetValue)
                    .build());
        }

        return new BalanceDTO(totalValue, assetDtoList);
    }

    private BalanceDTO calculateCurrentBalance(List<UserAsset> userAssetList) {
        // Load all assets from the DB at once to avoid spamming the DB with queries
        Map<String, Asset> assets = new HashMap<>();
        assetRepository.findAllById(
                userAssetList.stream().map(ua -> ua.getId().getAssetId()).toList()
        ).forEach(asset -> assets.put(asset.getId(), asset));

        BigDecimal totalValue = BigDecimal.ZERO;
        List<AssetDTO> assetDtoList = new ArrayList<>(userAssetList.size());
        for (UserAsset userAsset : userAssetList) {
            Asset asset = assets.get(userAsset.getId().getAssetId());
            if (asset == null) {
               throw new AssetNotFoundException("No asset found for symbol: " + userAsset.getId().getAssetId());
            }
            BigDecimal assetValue = userAsset.getQuantity().multiply(asset.getUsdPrice());
            totalValue = totalValue.add(assetValue);

            assetDtoList.add(AssetDTO.builder()
                    .symbol(userAsset.getId().getAssetId())
                    .quantity(userAsset.getQuantity())
                    .price(asset.getUsdPrice())
                    .value(assetValue)
                    .timestamp(asset.getUpdatedAt())
                    .build());
        }


        return new BalanceDTO(totalValue, assetDtoList);
    }

    @Transactional
    public void addAsset(@Valid WalletAddAssetRequestDTO walletAddAssetRequestDTO) {
        User currentUser = userService.getCurrentUser();

        String symbol = walletAddAssetRequestDTO.getSymbol().toUpperCase();

        userAssetRepository.findByIdUserIdAndIdAssetId(currentUser.getId(), symbol).ifPresent(userAsset -> {
            throw new AssetAlreadyExistsException("User already has asset: " + symbol);
        });

        AssetPrice assetPrice = assetPriceService.getAssetPrice(symbol);

        Asset asset = assetRepository.save(
                Asset.builder()
                        .id(symbol)
                        .usdPrice(assetPrice.getPrice())
                        .createdAt(assetPrice.getTimestamp())
                        .updatedAt(assetPrice.getTimestamp())
                        .build()
        );

        userAssetRepository.save(
                UserAsset.builder()
                        .id(new UserAsset.UserAssetId(currentUser.getId(), asset.getId()))
                        .asset(asset)
                        .user(currentUser)
                        .quantity(walletAddAssetRequestDTO.getQuantity())
                        .price(asset.getUsdPrice())
                        .build()
        );
    }

    @Transactional
    public void updateAsset(@Valid WalletAddAssetRequestDTO walletAddAssetRequestDTO) {
        User currentUser = userService.getCurrentUser();

        String symbol = walletAddAssetRequestDTO.getSymbol().toUpperCase();
        // We don't need to get its value from CoinCap because the symbol was already validated

        UserAsset userAsset = userAssetRepository.findByIdUserIdAndIdAssetId(
                currentUser.getId(), symbol
        ).orElseThrow(() -> new AssetNotFoundException("No user asset found for symbol: " + symbol));

        userAsset.setQuantity(walletAddAssetRequestDTO.getQuantity());
        userAsset.setPrice(walletAddAssetRequestDTO.getPrice());

        userAssetRepository.save(userAsset);
    }
}
