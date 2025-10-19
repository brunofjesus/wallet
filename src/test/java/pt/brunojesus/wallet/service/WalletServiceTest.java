package pt.brunojesus.wallet.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.brunojesus.wallet.dto.WalletAddAssetDTO;
import pt.brunojesus.wallet.entity.Asset;
import pt.brunojesus.wallet.entity.User;
import pt.brunojesus.wallet.entity.UserAsset;
import pt.brunojesus.wallet.exception.AssetAlreadyExistsException;
import pt.brunojesus.wallet.exception.AssetNotFoundException;
import pt.brunojesus.wallet.price.AssetPrice;
import pt.brunojesus.wallet.price.AssetPriceFetchingException;
import pt.brunojesus.wallet.price.AssetPriceService;
import pt.brunojesus.wallet.repository.AssetRepository;
import pt.brunojesus.wallet.repository.UserAssetRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private UserAssetRepository userAssetRepository;

    @Mock
    private AssetPriceService assetPriceService;

    @Mock
    private UserService userService;

    @InjectMocks
    private WalletService walletService;

    @Test
    @DisplayName("Should successfully add new asset to user wallet")
    void addAsset_Success() {
        // Mocks
        UUID mockUserId = UUID.randomUUID();
        User mockUser = new User();
        mockUser.setId(mockUserId);

        AssetPrice mockAssetPrice = new AssetPrice(
                Instant.ofEpochSecond(1790863200),
                new BigDecimal(500_000)
        );

        Asset mockAsset = Asset.builder().id("BTC")
                .usdPrice(mockAssetPrice.getPrice())
                .createdAt(mockAssetPrice.getTimestamp())
                .updatedAt(mockAssetPrice.getTimestamp())
                .build();

        // Given
        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(userAssetRepository.findByIdUserIdAndIdAssetId(mockUserId, "BTC"))
                .thenReturn(Optional.empty());
        when(assetPriceService.getAssetPriceBySymbol("BTC")).thenReturn(mockAssetPrice);
        when(assetRepository.save(eq(mockAsset))).thenReturn(mockAsset);

        // When
        walletService.addAsset(
                new WalletAddAssetDTO("BTC", new BigDecimal(100_000), new BigDecimal(2))
        );

        // Then
        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        ArgumentCaptor<UserAsset> userAssetCaptor = ArgumentCaptor.forClass(UserAsset.class);

        verify(userService).getCurrentUser();
        verify(userAssetRepository).findByIdUserIdAndIdAssetId(mockUserId, "BTC");
        verify(assetPriceService).getAssetPriceBySymbol("BTC");
        verify(assetRepository).save(assetCaptor.capture());
        verify(userAssetRepository).save(userAssetCaptor.capture());

        // Assert captured asset
        Asset capturedAsset = assetCaptor.getValue();
        assertEquals("BTC", capturedAsset.getId());
        assertEquals(new BigDecimal(500_000), capturedAsset.getUsdPrice());
        assertEquals(mockAssetPrice.getTimestamp(), capturedAsset.getCreatedAt());
        assertEquals(mockAssetPrice.getTimestamp(), capturedAsset.getUpdatedAt());

        // Assert captured user asset
        UserAsset capturedUserAsset = userAssetCaptor.getValue();
        assertEquals("BTC", capturedUserAsset.getId().getAssetId());
        assertEquals(mockUserId, capturedUserAsset.getId().getUserId());
        assertEquals(mockUser, capturedUserAsset.getUser());
        assertEquals(mockAsset, capturedUserAsset.getAsset());
        assertEquals(new BigDecimal(2), capturedUserAsset.getAmount());
        assertEquals(new BigDecimal(500_000), capturedUserAsset.getPrice());
    }

    @Test
    @DisplayName("Should fail to add new asset to user wallet when not logged in")
    void addAsset_NotLoggedIn() {
        when(userService.getCurrentUser()).thenThrow(new RuntimeException("Not logged in"));
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> walletService.addAsset(
                        new WalletAddAssetDTO("BTC", new BigDecimal(100_000), new BigDecimal(2))
                )
        );

        verify(assetRepository, never()).save(any(Asset.class));
        verify(userAssetRepository, never()).save(any(UserAsset.class));

        assertEquals("Not logged in", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail to add new asset to user wallet when asset already exists")
    void addAsset_AlreadyExists() {
        UUID mockUserId = UUID.randomUUID();
        User mockUser = new User();
        mockUser.setId(mockUserId);

        // Given
        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(userAssetRepository.findByIdUserIdAndIdAssetId(mockUserId, "BTC"))
                .thenReturn(Optional.of(UserAsset.builder().build()));

        // When ... Then
        AssetAlreadyExistsException exception = assertThrowsExactly(
                AssetAlreadyExistsException.class,
                () -> walletService.addAsset(
                        new WalletAddAssetDTO("BTC", new BigDecimal(100_000), new BigDecimal(2))
                )
        );

        verify(assetRepository, never()).save(any(Asset.class));
        verify(userAssetRepository, never()).save(any(UserAsset.class));

        assertEquals("User already has asset: BTC", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail to add new asset to user wallet when asset price fetching fails")
    void addAsset_AssetNotFound() {
        UUID mockUserId = UUID.randomUUID();
        User mockUser = new User();
        mockUser.setId(mockUserId);

        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(userAssetRepository.findByIdUserIdAndIdAssetId(mockUserId, "BTC"))
                .thenReturn(Optional.empty());
        when(assetPriceService.getAssetPriceBySymbol("BTC")).thenThrow(new AssetPriceFetchingException("Asset not found"));


        AssetPriceFetchingException exception = assertThrowsExactly(
                AssetPriceFetchingException.class,
                () -> walletService.addAsset(
                        new WalletAddAssetDTO("BTC", new BigDecimal(100_000), new BigDecimal(2))
                )
        );

        verify(assetRepository, never()).save(any(Asset.class));
        verify(userAssetRepository, never()).save(any(UserAsset.class));

        assertEquals("Asset not found", exception.getMessage());
    }


    @Test
    @DisplayName("Should successfully update user wallet asset")
    void updateAsset_Success() {
        // Mocks
        UUID mockUserId = UUID.randomUUID();
        User mockUser = new User();
        mockUser.setId(mockUserId);

        Instant now = Instant.now();
        UserAsset mockUserAsset = UserAsset.builder()
                .id(new UserAsset.UserAssetId(mockUserId, "BTC"))
                .user(mockUser)
                .asset(Asset.builder().id("BTC").usdPrice(new BigDecimal(1_000_000)).build())
                .price(new BigDecimal(200_000))
                .updatedAt(now)
                .createdAt(now)
                .build();

        // Given
        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(userAssetRepository.findByIdUserIdAndIdAssetId(mockUserId, "BTC"))
                .thenReturn(Optional.of(mockUserAsset));

        // When
        walletService.updateAsset(
                new WalletAddAssetDTO("BTC", new BigDecimal(100_000), new BigDecimal(2))
        );

        // Then
        ArgumentCaptor<UserAsset> userAssetCaptor = ArgumentCaptor.forClass(UserAsset.class);

        verify(userService).getCurrentUser();
        verify(userAssetRepository).findByIdUserIdAndIdAssetId(mockUserId, "BTC");
        verify(userAssetRepository).save(userAssetCaptor.capture());

        verify(assetPriceService, never()).getAssetPriceBySymbol("BTC");
        verify(assetRepository, never()).save(any(Asset.class));

        // Assert captured user asset
        UserAsset capturedUserAsset = userAssetCaptor.getValue();
        assertEquals("BTC", capturedUserAsset.getId().getAssetId());
        assertEquals(mockUserId, capturedUserAsset.getId().getUserId());
        assertEquals(mockUserAsset.getUser(), capturedUserAsset.getUser());
        assertEquals(mockUserAsset.getAsset(), capturedUserAsset.getAsset());
        assertEquals(new BigDecimal(2), capturedUserAsset.getAmount());
        assertEquals(new BigDecimal(100_000), capturedUserAsset.getPrice());
    }

    @Test
    @DisplayName("Should fail to update user wallet asset when not logged in")
    void updateAsset_NotLoggedIn() {
        when(userService.getCurrentUser()).thenThrow(new RuntimeException("Not logged in"));
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> walletService.updateAsset(
                        new WalletAddAssetDTO("BTC", new BigDecimal(100_000), new BigDecimal(2))
                )
        );

        verify(assetRepository, never()).save(any(Asset.class));

        assertEquals("Not logged in", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail to update user wallet asset when asset not found")
    void updateAsset_AssetNotFound() {
        UUID mockUserId = UUID.randomUUID();
        User mockUser = new User();
        mockUser.setId(mockUserId);

        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(userAssetRepository.findByIdUserIdAndIdAssetId(mockUserId, "BTC"))
                .thenReturn(Optional.empty());

        AssetNotFoundException exception = assertThrowsExactly(
                AssetNotFoundException.class,
                () -> walletService.updateAsset(
                        new WalletAddAssetDTO("BTC", new BigDecimal(100_000), new BigDecimal(2))
                )
        );

        assertEquals("No user asset found for symbol: BTC", exception.getMessage());
    }

    @Test
    @DisplayName("Should convert symbol to uppercase when adding asset")
    void addAsset_SymbolConvertedToUppercase() {
        // Mocks
        UUID mockUserId = UUID.randomUUID();
        User mockUser = new User();
        mockUser.setId(mockUserId);

        AssetPrice mockAssetPrice = new AssetPrice(
                Instant.ofEpochSecond(1790863200),
                new BigDecimal(3000)
        );

        Asset mockAsset = Asset.builder().id("ETH")
                .usdPrice(mockAssetPrice.getPrice())
                .createdAt(mockAssetPrice.getTimestamp())
                .updatedAt(mockAssetPrice.getTimestamp())
                .build();

        // Given
        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(userAssetRepository.findByIdUserIdAndIdAssetId(mockUserId, "ETH"))
                .thenReturn(Optional.empty());
        when(assetPriceService.getAssetPriceBySymbol("ETH")).thenReturn(mockAssetPrice);
        when(assetRepository.save(any(Asset.class))).thenReturn(mockAsset);

        // When - pass lowercase symbol
        walletService.addAsset(
                new WalletAddAssetDTO("eth", new BigDecimal(3000), new BigDecimal(1))
        );

        // Then - verify uppercase symbol was used
        verify(userAssetRepository).findByIdUserIdAndIdAssetId(mockUserId, "ETH");
        verify(assetPriceService).getAssetPriceBySymbol("ETH");

        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(assetCaptor.capture());
        assertEquals("ETH", assetCaptor.getValue().getId());
    }
}