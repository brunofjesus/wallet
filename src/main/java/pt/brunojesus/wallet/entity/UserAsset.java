package pt.brunojesus.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * UserAsset entity.
 * Represents the relationship between a user and an asset with quantity.
 * This acts as the implicit wallet - each user can have multiple assets with quantities.
 * Uses an embedded composite key to avoid duplicated state. 
 * @author bruno
 */
@Entity
@Table(name = "user_asset")
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserAsset {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private UserAssetId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("assetId")
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(name = "quantity", precision = 20, scale = 8, nullable = false)
    private BigDecimal quantity;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UserAsset(UUID userId, String assetId, BigDecimal quantity) {
        this.id = new UserAssetId(userId, assetId);
        this.quantity = quantity;
    }

    public UserAsset(User user, Asset asset, BigDecimal quantity) {
        this.user = user;
        this.asset = asset;
        this.id = new UserAssetId(user != null ? user.getId() : null, asset != null ? asset.getId() : null);
        this.quantity = quantity;
    }

    /**
     * Composite key class for UserAsset entity.
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAssetId implements Serializable {

        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "asset_id", length = 10)
        private String assetId;

        public UUID getUserId() {
            return userId;
        }

        public void setUserId(UUID userId) {
            this.userId = userId;
        }

        public String getAssetId() {
            return assetId;
        }

        public void setAssetId(String assetId) {
            this.assetId = assetId;
        }
    }
}