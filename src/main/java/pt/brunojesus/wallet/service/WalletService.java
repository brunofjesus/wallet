package pt.brunojesus.wallet.service;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.brunojesus.wallet.dto.WalletAddAssetDTO;
import pt.brunojesus.wallet.entity.Asset;
import pt.brunojesus.wallet.entity.User;
import pt.brunojesus.wallet.entity.UserAsset;
import pt.brunojesus.wallet.exception.AssetAlreadyExistsException;
import pt.brunojesus.wallet.exception.AssetNotFoundException;
import pt.brunojesus.wallet.price.AssetPrice;
import pt.brunojesus.wallet.price.AssetPriceService;
import pt.brunojesus.wallet.repository.AssetRepository;
import pt.brunojesus.wallet.repository.UserAssetRepository;


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

    @Transactional
    public void addAsset(@Valid WalletAddAssetDTO walletAddAssetDTO) {
        User currentUser = userService.getCurrentUser();

        String symbol = walletAddAssetDTO.getSymbol().toUpperCase();

        userAssetRepository.findByIdUserIdAndIdAssetId(currentUser.getId(), symbol).ifPresent(userAsset -> {
            throw new AssetAlreadyExistsException("User already has asset: " + symbol);
        });

        AssetPrice assetPrice = assetPriceService.getAssetPriceBySymbol(symbol);

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
                        .amount(walletAddAssetDTO.getAmount())
                        .price(asset.getUsdPrice())
                        .build()
        );
    }

    @Transactional
    public void updateAsset(@Valid WalletAddAssetDTO walletAddAssetDTO) {
        User currentUser = userService.getCurrentUser();

        String symbol = walletAddAssetDTO.getSymbol().toUpperCase();
        // We don't need to get its value from CoinCap because the symbol was already validated

        UserAsset userAsset = userAssetRepository.findByIdUserIdAndIdAssetId(
                currentUser.getId(), symbol
        ).orElseThrow(() -> new AssetNotFoundException("No user asset found for symbol: " + symbol));

        userAsset.setAmount(walletAddAssetDTO.getAmount());
        userAsset.setPrice(walletAddAssetDTO.getPrice());

        userAssetRepository.save(userAsset);
    }
}
