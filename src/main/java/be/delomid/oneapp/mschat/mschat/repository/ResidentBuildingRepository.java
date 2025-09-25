package be.delomid.oneapp.mschat.mschat.repository;

import be.delomid.oneapp.mschat.mschat.model.ResidentBuilding;
import be.delomid.oneapp.mschat.mschat.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResidentBuildingRepository extends JpaRepository<ResidentBuilding, Long> {

    @Query("SELECT rb FROM ResidentBuilding rb WHERE rb.resident.idUsers = :residentId AND rb.isActive = true")
    List<ResidentBuilding> findActiveByResidentId(@Param("residentId") String residentId);

    @Query("SELECT rb FROM ResidentBuilding rb WHERE rb.resident.email = :email AND rb.isActive = true")
    List<ResidentBuilding> findActiveByResidentEmail(@Param("email") String email);

    @Query("SELECT rb FROM ResidentBuilding rb WHERE rb.resident.idUsers = :residentId AND rb.building.buildingId = :buildingId AND rb.isActive = true")
    Optional<ResidentBuilding> findByResidentIdAndBuildingId(@Param("residentId") String residentId, @Param("buildingId") String buildingId);

    @Query("SELECT rb FROM ResidentBuilding rb WHERE rb.resident.email = :email AND rb.building.buildingId = :buildingId AND rb.isActive = true")
    Optional<ResidentBuilding> findByResidentEmailAndBuildingId(@Param("email") String email, @Param("buildingId") String buildingId);

    @Query("SELECT rb FROM ResidentBuilding rb WHERE rb.building.buildingId = :buildingId AND rb.isActive = true")
    List<ResidentBuilding> findActiveByBuildingId(@Param("buildingId") String buildingId);

    @Query("SELECT rb FROM ResidentBuilding rb WHERE rb.building.buildingId = :buildingId AND rb.roleInBuilding = :role AND rb.isActive = true")
    List<ResidentBuilding> findByBuildingIdAndRole(@Param("buildingId") String buildingId, @Param("role") UserRole role);
}