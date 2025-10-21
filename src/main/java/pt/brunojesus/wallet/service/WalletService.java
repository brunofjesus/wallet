package pt.brunojesus.wallet.service;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
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
import pt.brunojesus.wallet.repository.UserAssetRepository;

import java.math.BigDecimal;
import java.util.*;


/**
 * Service for managing user wallet operations including asset management and balance calculations.
 *
 * <p>This service provides functionality for:
 * <ul>
 *   <li>Adding new assets to user wallets</li>
 *   <li>Updating existing asset quantities and prices</li>
 *   <li>Calculating portfolio values at purchase and current market prices</li>
 * </ul>
 *
 * <p>All operations are performed in the context of the currently authenticated user.
 *
 * @author bruno
 * @see AssetService
 * @see UserService
 * @see UserAssetRepository
 */
@Service
public class WalletService {

    private final AssetService assetService;
    private final UserAssetRepository userAssetRepository;
    private final UserService userService;

    /**
     * Constructs a new WalletService with the required dependencies.
     *
     * @param assetService the service for managing asset operations
     * @param userAssetRepository the repository for user asset data access
     * @param userService the service for user operations
     */
    @Autowired
    public WalletService(
            AssetService assetService,
            UserAssetRepository userAssetRepository,
            UserService userService
    ) {
        this.assetService = assetService;
        this.userAssetRepository = userAssetRepository;
        this.userService = userService;
    }

    /**
     * Retrieves comprehensive wallet information for the current user.
     *
     * <p>This method calculates both the original balance (based on purchase prices)
     * and current balance (based on current market prices) for all assets in the
     * user's wallet.
     *
     * @return a {@link WalletInfoDTO} containing the user ID, original balance, and current balance
     * @throws AssetNotFoundException if any asset in the wallet cannot be found in the database
     * @see #calculateOriginalBalance(List)
     * @see #calculateCurrentBalance(List)
     */
    @Transactional(readOnly = true)
    public WalletInfoDTO info() {
        User currentUser = userService.getCurrentUser();

        List<UserAsset> userAssetsList = userAssetRepository.findByIdUserId(currentUser.getId());

        BalanceDTO originalBalance = calculateOriginalBalance(userAssetsList);
        BalanceDTO currentBalance = calculateCurrentBalance(userAssetsList);

        return new WalletInfoDTO(currentUser.getId(), originalBalance, currentBalance);
    }

    /**
     * Calculates the original balance of the user's portfolio based on purchase prices.
     *
     * <p>This method computes the total value of all assets using the prices at which
     * they were originally purchased by the user.
     *
     * @param userAssetList the list of user assets to calculate the balance for
     * @return a {@link BalanceDTO} containing the total original value and individual asset details
     */
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

    /**
     * Calculates the current balance of the user's portfolio based on current market prices.
     *
     * <p>This method retrieves the latest market prices for all assets in the user's
     * portfolio and calculates their current total value. To optimize performance,
     * all required assets are loaded from the database in a single operation.
     *
     * @param userAssetList the list of user assets to calculate the balance for
     * @return a {@link BalanceDTO} containing the total current value and individual asset details
     * @throws AssetNotFoundException if any required asset is not found in the database
     */
    private BalanceDTO calculateCurrentBalance(List<UserAsset> userAssetList) {
        // Load all assets from the DB at once to avoid spamming the DB with queries
        List<String> assetIds = userAssetList.stream()
                .map(ua -> ua.getId().getAssetId()).toList();

        Map<String, Asset> assets = new HashMap<>();
        assetService.findByIds(assetIds)
                .forEach(asset -> assets.put(asset.getId(), asset));

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

    /**
     * Adds a new asset to the current user's wallet.
     *
     * <p>This method creates a new user asset entry with the specified quantity and price.
     * The asset symbol is automatically converted to uppercase for consistency.
     * If the asset doesn't exist in the system, it will be created automatically.
     *
     * @param walletAddAssetRequestDTO the request containing asset symbol, quantity, and purchase price
     * @throws AssetAlreadyExistsException if the user already has this asset in their wallet
     * @see AssetService#findOrCreateAsset(String)
     */
    @Transactional
    public void addAsset(@Valid WalletAddAssetRequestDTO walletAddAssetRequestDTO) {
        User currentUser = userService.getCurrentUser();

        String symbol = walletAddAssetRequestDTO.getSymbol().toUpperCase();

        userAssetRepository.findByIdUserIdAndIdAssetId(currentUser.getId(), symbol).ifPresent(userAsset -> {
            throw new AssetAlreadyExistsException("User already has asset: " + symbol);
        });

        Asset asset = assetService.findOrCreateAsset(symbol);


        userAssetRepository.save(
                UserAsset.builder()
                        .id(new UserAsset.UserAssetId(currentUser.getId(), asset.getId()))
                        .asset(asset)
                        .user(currentUser)
                        .quantity(walletAddAssetRequestDTO.getQuantity())
                        .price(walletAddAssetRequestDTO.getPrice())
                        .build()
        );
    }

    /**
     * Updates an existing asset in the current user's wallet.
     *
     * <p>This method modifies the quantity and price of an existing user asset.
     * The asset symbol is automatically converted to uppercase for consistency.
     * The asset must already exist in the user's wallet.
     *
     * @param walletAddAssetRequestDTO the request containing asset symbol, new quantity, and new price
     * @throws AssetNotFoundException if the user doesn't have this asset in their wallet
     */
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
