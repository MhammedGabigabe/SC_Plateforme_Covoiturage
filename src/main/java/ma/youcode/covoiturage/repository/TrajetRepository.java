package ma.youcode.covoiturage.repository;

import ma.youcode.covoiturage.entity.Trajet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TrajetRepository extends JpaRepository<Trajet, Long> {

    boolean existsByVehiculeId(Long vehiculeId);

    @Query("""
            SELECT t FROM Trajet t
            JOIN FETCH t.vehicule v
            JOIN FETCH v.conducteur
            LEFT JOIN FETCH t.pointsArret
            WHERE t.id = :id
            """)
    Optional<Trajet> findDetailById(@Param("id") Long id);
}
