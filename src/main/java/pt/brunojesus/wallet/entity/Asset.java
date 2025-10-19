package pt.brunojesus.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Asset entity.
 * Representation of an asset that can be included in the wallet.
 * Uses Lombok to reduce boilerplate. Equals/HashCode are based on id.
 *
 * @author bruno
 */
@Entity
@Table(name = "asset")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Asset {

    @Id
    @Column(name = "id", length = 10)
    @EqualsAndHashCode.Include
    private String id;

    @Column(name = "usd_price", precision = 20, scale = 8, nullable = false)
    private BigDecimal usdPrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}