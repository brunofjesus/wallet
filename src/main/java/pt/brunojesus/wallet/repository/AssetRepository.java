package pt.brunojesus.wallet.repository;

import pt.brunojesus.wallet.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.stream.Stream;

@Repository
public interface AssetRepository extends JpaRepository<Asset, String> {
    
    @Query("SELECT a FROM Asset a")
    Stream<Asset> streamAll();
}