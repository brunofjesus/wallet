package pt.brunojesus.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Asset entity.
 * Representation of an asset that can be included in the wallet.
 * Uses Lombok to reduce boilerplate. Equals/HashCode are based on id.
 *
 * @author bruno
 */
@Entity
@Table(name = "asset")
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

    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
}