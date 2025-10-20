package pt.brunojesus.wallet.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.brunojesus.wallet.entity.Asset;
import pt.brunojesus.wallet.price.AssetPrice;
import pt.brunojesus.wallet.price.AssetPriceService;
import pt.brunojesus.wallet.repository.AssetRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private AssetPriceService assetPriceService;

    @InjectMocks
    private AssetService assetService;

    @Test
    @DisplayName("Should successfully stream all assets from the database")
    void streamAll_Success() {
        Stream<Asset> stream = Stream.<Asset>builder().build();
        when(assetRepository.streamAll()).thenReturn(stream);

        Stream<Asset> result = assetService.streamAll();

        assertSame(stream, result);
    }

    @Test
    @DisplayName("Should successfully find assets by their IDs from the database")
    void findByIds_Success() {
        List<String> ids = List.of("BTC", "ETH");
        List<Asset> assets = List.of(mock(Asset.class), mock(Asset.class));
        when(assetRepository.findAllById(ids)).thenReturn(assets);

        List<Asset> result = assetService.findByIds(ids);

        assertSame(assets, result);
    }

    @Test
    @DisplayName("Should successfully find an asset by its ID from the database")
    void findOrCreateAsset_Success_InDatabase() {
        Asset mockAsset = mock(Asset.class);
        when(assetRepository.findById("BTC")).thenReturn(Optional.of(mockAsset));

        Asset result = assetService.findOrCreateAsset("BTC");

        assertSame(mockAsset, result);
    }

    @Test
    @DisplayName("Should successfully get, create and save a new asset to the database")
    void findOrCreateAsset_Success_NotInDatabase() {

        Instant now = Instant.now();
        AssetPrice mockAssetPrice = AssetPrice.builder()
                .timestamp(now)
                .price(BigDecimal.TEN)
                .build();
        Asset expectedAsset = Asset.builder()
                        .id("BTC").usdPrice(BigDecimal.TEN)
                        .createdAt(now).updatedAt(now)
                        .build();

        when(assetRepository.findById("BTC")).thenReturn(Optional.empty());
        when(assetPriceService.getAssetPrice("BTC")).thenReturn(mockAssetPrice);
        when(assetRepository.save(expectedAsset)).thenReturn(expectedAsset);

        Asset result = assetService.findOrCreateAsset("BTC");

        assertSame(expectedAsset, result);

        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(assetCaptor.capture());

        assertEquals(expectedAsset, assetCaptor.getValue());
        assertEquals("BTC", assetCaptor.getValue().getId());
        assertEquals(BigDecimal.TEN, assetCaptor.getValue().getUsdPrice());
        assertEquals(now, assetCaptor.getValue().getCreatedAt());
        assertEquals(now, assetCaptor.getValue().getUpdatedAt());
    }

    @Test
    @DisplayName("Should fail to get an asset from when fetching fails")
    void findOrCreateAsset_AssetPriceFetchingFails() {
        when(assetRepository.findById("BTC")).thenReturn(Optional.empty());
        when(assetPriceService.getAssetPrice("BTC")).thenThrow(new RuntimeException("Failed to fetch asset price"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> assetService.findOrCreateAsset("BTC")
        );

        assertEquals("Failed to fetch asset price", exception.getMessage());

        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    @DisplayName("Should successfully refresh asset price")
    void refreshAssetPrice_Success() {
        AssetPrice mockAssetPrice = AssetPrice.builder().price(BigDecimal.TEN).build();
        when(assetPriceService.getAssetPrice("BTC")).thenReturn(mockAssetPrice);

        assetService.refreshAssetPrice(Asset.builder().id("BTC").build());

        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository, times(1)).save(assetCaptor.capture());

        assertEquals(BigDecimal.TEN, assetCaptor.getValue().getUsdPrice());
    }

    @Test
    @DisplayName("Should not crash when refreshing asset price fails")
    void refreshAssetPrice_AssetPriceFetchingFails() {
        when(assetPriceService.getAssetPrice("BTC")).thenThrow(new RuntimeException("Failed to fetch asset price"));

        assertDoesNotThrow(
                () -> assetService.refreshAssetPrice(Asset.builder().id("BTC").build())
        );

    }
}