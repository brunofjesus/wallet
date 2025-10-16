package pt.brunojesus.wallet.repository;

import pt.brunojesus.wallet.entity.UserAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAssetRepository extends JpaRepository<UserAsset, UserAsset.UserAssetId> {
    
    List<UserAsset> findByIdUserId(UUID userId);
    
    Optional<UserAsset> findByIdUserIdAndIdAssetId(UUID userId, String assetId);
    
    @Query("SELECT ua FROM UserAsset ua JOIN FETCH ua.asset WHERE ua.id.userId = :userId")
    List<UserAsset> findByUserIdWithAsset(@Param("userId") UUID userId);
    
    boolean existsByIdUserIdAndIdAssetId(UUID userId, String assetId);
    
    void deleteByIdUserIdAndIdAssetId(UUID userId, String assetId);
}