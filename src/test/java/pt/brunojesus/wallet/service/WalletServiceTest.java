package pt.brunojesus.wallet.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.brunojesus.wallet.dto.WalletAddAssetRequestDTO;
import pt.brunojesus.wallet.dto.WalletInfoDTO;
import pt.brunojesus.wallet.entity.Asset;
import pt.brunojesus.wallet.entity.User;
import pt.brunojesus.wallet.entity.UserAsset;
import pt.brunojesus.wallet.exception.AssetAlreadyExistsException;
import pt.brunojesus.wallet.exception.AssetNotFoundException;
import pt.brunojesus.wallet.price.AssetPrice;
import pt.brunojesus.wallet.price.AssetPriceFetchingException;
import pt.brunojesus.wallet.repository.UserAssetRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private AssetService assetService;

    @Mock
    private UserAssetRepository userAssetRepository;

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
        when(assetService.findOrCreateAsset("BTC")).thenReturn(mockAsset);

        // When
        walletService.addAsset(
                new WalletAddAssetRequestDTO("BTC", new BigDecimal(100_000), new BigDecimal(2))
        );

        // Then
        ArgumentCaptor<UserAsset> userAssetCaptor = ArgumentCaptor.forClass(UserAsset.class);

        verify(userService).getCurrentUser();
        verify(userAssetRepository).findByIdUserIdAndIdAssetId(mockUserId, "BTC");
        verify(userAssetRepository).save(userAssetCaptor.capture());

        // Assert captured user asset
        UserAsset capturedUserAsset = userAssetCaptor.getValue();
        assertEquals("BTC", capturedUserAsset.getId().getAssetId());
        assertEquals(mockUserId, capturedUserAsset.getId().getUserId());
        assertEquals(mockUser, capturedUserAsset.getUser());
        assertEquals(mockAsset, capturedUserAsset.getAsset());
        assertEquals(new BigDecimal(2), capturedUserAsset.getQuantity());
        assertEquals(new BigDecimal(100_000), capturedUserAsset.getPrice());
    }

    @Test
    @DisplayName("Should fail to add new asset to user wallet when not logged in")
    void addAsset_NotLoggedIn() {
        when(userService.getCurrentUser()).thenThrow(new RuntimeException("Not logged in"));
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> walletService.addAsset(
                        new WalletAddAssetRequestDTO("BTC", new BigDecimal(100_000), new BigDecimal(2))
                )
        );

        verify(assetService, never()).findOrCreateAsset("BTC");
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
                        new WalletAddAssetRequestDTO("BTC", new BigDecimal(100_000), new BigDecimal(2))
                )
        );

        verify(assetService, never()).findOrCreateAsset("BTC");
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
        when(assetService.findOrCreateAsset("BTC")).thenThrow(new AssetPriceFetchingException("Asset not found"));


        AssetPriceFetchingException exception = assertThrowsExactly(
                AssetPriceFetchingException.class,
                () -> walletService.addAsset(
                        new WalletAddAssetRequestDTO("BTC", new BigDecimal(100_000), new BigDecimal(2))
                )
        );

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
                new WalletAddAssetRequestDTO("BTC", new BigDecimal(100_000), new BigDecimal(2))
        );

        // Then
        ArgumentCaptor<UserAsset> userAssetCaptor = ArgumentCaptor.forClass(UserAsset.class);

        verify(userService).getCurrentUser();
        verify(userAssetRepository).findByIdUserIdAndIdAssetId(mockUserId, "BTC");
        verify(userAssetRepository).save(userAssetCaptor.capture());

        verify(assetService, never()).findOrCreateAsset("BTC");

        // Assert captured user asset
        UserAsset capturedUserAsset = userAssetCaptor.getValue();
        assertEquals("BTC", capturedUserAsset.getId().getAssetId());
        assertEquals(mockUserId, capturedUserAsset.getId().getUserId());
        assertEquals(mockUserAsset.getUser(), capturedUserAsset.getUser());
        assertEquals(mockUserAsset.getAsset(), capturedUserAsset.getAsset());
        assertEquals(new BigDecimal(2), capturedUserAsset.getQuantity());
        assertEquals(new BigDecimal(100_000), capturedUserAsset.getPrice());
    }

    @Test
    @DisplayName("Should fail to update user wallet asset when not logged in")
    void updateAsset_NotLoggedIn() {
        when(userService.getCurrentUser()).thenThrow(new RuntimeException("Not logged in"));
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> walletService.updateAsset(
                        new WalletAddAssetRequestDTO("BTC", new BigDecimal(100_000), new BigDecimal(2))
                )
        );

        verify(assetService, never()).findOrCreateAsset("BTC");

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
                        new WalletAddAssetRequestDTO("BTC", new BigDecimal(100_000), new BigDecimal(2))
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
        when(assetService.findOrCreateAsset("ETH")).thenReturn(mockAsset);

        // When - pass lowercase symbol
        walletService.addAsset(
                new WalletAddAssetRequestDTO("eth", new BigDecimal(3000), new BigDecimal(1))
        );

        // Then - verify uppercase symbol was used
        verify(userAssetRepository).findByIdUserIdAndIdAssetId(mockUserId, "ETH");
        verify(assetService).findOrCreateAsset("ETH");
    }

    @Test
    @DisplayName("Should successfully get wallet info")
    void info_Success() {
        UUID mockUserId = UUID.randomUUID();
        User mockUser = new User();
        mockUser.setId(mockUserId);

        Asset mockBtcAsset = Asset.builder().id("BTC").usdPrice(new BigDecimal(500_000)).build();
        Asset mockEthAsset = Asset.builder().id("ETH").usdPrice(new BigDecimal(3000)).build();
        Instant now = Instant.now();

        // Given
        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(userAssetRepository.findByIdUserId(mockUserId)).thenReturn(
                List.of(
                        UserAsset.builder()
                                .id(new UserAsset.UserAssetId(mockUserId, "BTC"))
                                .user(mockUser)
                                .asset(mockBtcAsset)
                                .quantity(BigDecimal.ONE)
                                .price(new BigDecimal(100_000))
                                .createdAt(now)
                                .updatedAt(now)
                                .build(),
                        UserAsset.builder()
                                .id(new UserAsset.UserAssetId(mockUserId, "ETH"))
                                .user(mockUser)
                                .asset(mockEthAsset)
                                .quantity(BigDecimal.TWO)
                                .price(new BigDecimal(3_000))
                                .createdAt(now)
                                .updatedAt(now)
                                .build()
                )
        );
        when(assetService.findByIds(List.of("BTC", "ETH"))).thenReturn(
                List.of(mockBtcAsset, mockEthAsset)
        );

        // When
        WalletInfoDTO result = walletService.info();

        // Then
        verify(userService).getCurrentUser();
        verify(userAssetRepository, times(1)).findByIdUserId(mockUserId);

        assertEquals(mockUserId, result.getId());
        assertNotNull(result.getOriginal());
        assertEquals(new BigDecimal(106_000), result.getOriginal().getTotal());
        assertEquals(2, result.getOriginal().getAssets().size());

        var originalBtcAsset = result.getOriginal().getAssets().get(0);
        assertEquals("BTC", originalBtcAsset.getSymbol());
        assertEquals(BigDecimal.ONE, originalBtcAsset.getQuantity());
        assertEquals(new BigDecimal(100_000), originalBtcAsset.getPrice());
        assertEquals(new BigDecimal(100_000), originalBtcAsset.getValue());
        assertNull(originalBtcAsset.getTimestamp());

        var originalEthAsset = result.getOriginal().getAssets().get(1);
        assertEquals("ETH", originalEthAsset.getSymbol());
        assertEquals(BigDecimal.TWO, originalEthAsset.getQuantity());
        assertEquals(new BigDecimal(3_000), originalEthAsset.getPrice());
        assertEquals(new BigDecimal(6_000), originalEthAsset.getValue());
        assertNull(originalEthAsset.getTimestamp());
    }

    @Test
    @DisplayName("Should fail to get wallet info when not logged in")
    void info_NotLoggedIn() {
        when(userService.getCurrentUser()).thenThrow(new RuntimeException("Not logged in"));
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> walletService.info()
        );

        verify(userAssetRepository, never()).findByIdUserId(any(UUID.class));

        assertEquals("Not logged in", exception.getMessage());
    }

    @Test
    @DisplayName("Should get wallet zero wallet info when user has no assets")
    void info_NoAssets() {
        UUID mockUserId = UUID.randomUUID();
        User mockUser = new User();
        mockUser.setId(mockUserId);

        // Given
        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(userAssetRepository.findByIdUserId(mockUserId)).thenReturn(List.of());

        // When
        WalletInfoDTO result = walletService.info();

        // Then
        verify(userService).getCurrentUser();
        verify(userAssetRepository, times(1)).findByIdUserId(mockUserId);

        assertEquals(mockUserId, result.getId());
        assertNotNull(result.getOriginal());
        assertEquals(new BigDecimal(0), result.getOriginal().getTotal());
        assertEquals(0, result.getOriginal().getAssets().size());

        assertNotNull(result.getCurrent());
        assertEquals(new BigDecimal(0), result.getCurrent().getTotal());
        assertEquals(0, result.getCurrent().getAssets().size());
    }

    @Test
    @DisplayName("Should fail if an asset is not found")
    void info_AssetNotFound() {
        UUID mockUserId = UUID.randomUUID();
        User mockUser = new User();
        mockUser.setId(mockUserId);

        Asset mockBtcAsset = Asset.builder().id("BTC").usdPrice(new BigDecimal(500_000)).build();
        Asset mockEthAsset = Asset.builder().id("ETH").usdPrice(new BigDecimal(3000)).build();
        Instant now = Instant.now();

        // Given
        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(userAssetRepository.findByIdUserId(mockUserId)).thenReturn(
                List.of(
                        UserAsset.builder()
                                .id(new UserAsset.UserAssetId(mockUserId, "BTC"))
                                .user(mockUser)
                                .asset(mockBtcAsset)
                                .quantity(BigDecimal.ONE)
                                .price(new BigDecimal(100_000))
                                .createdAt(now)
                                .updatedAt(now)
                                .build(),
                        UserAsset.builder()
                                .id(new UserAsset.UserAssetId(mockUserId, "ETH"))
                                .user(mockUser)
                                .asset(mockEthAsset)
                                .quantity(BigDecimal.TWO)
                                .price(new BigDecimal(3_000))
                                .createdAt(now)
                                .updatedAt(now)
                                .build()
                )
        );
        when(assetService.findByIds(List.of("BTC", "ETH"))).thenReturn(
                List.of(mockBtcAsset)
        );

        // When ... Then
        AssetNotFoundException exception = assertThrowsExactly(
                AssetNotFoundException.class,
                () -> walletService.info()
        );

        assertEquals("No asset found for symbol: ETH", exception.getMessage());
    }
}