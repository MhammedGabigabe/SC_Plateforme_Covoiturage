package ma.youcode.covoiturage.repository;

import jakarta.persistence.LockModeType;
import ma.youcode.covoiturage.dto.response.TrajetRechercheResponse;
import ma.youcode.covoiturage.entity.Trajet;
import ma.youcode.covoiturage.entity.enums.StatutReservation;
import ma.youcode.covoiturage.entity.enums.StatutTrajet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Query(value = """
            SELECT new ma.youcode.covoiturage.dto.response.TrajetRechercheResponse(
                t.id,
                depart.ville,
                arrivee.ville,
                depart.heurePassage,
                arrivee.heurePassage,
                (arrivee.prixDepuisDepart - depart.prixDepuisDepart),
                (t.placesProposees - COALESCE(
                    (SELECT SUM(r.nombrePlaces) FROM Reservation r
                     WHERE r.trajet = t AND r.statut = :statutReservation), 0L)),
                c.nom,
                c.prenom,
                v.marque,
                v.modele)
            FROM Trajet t
            JOIN t.vehicule v
            JOIN v.conducteur c
            JOIN t.pointsArret depart
            JOIN t.pointsArret arrivee
            WHERE t.statut = :statutTrajet
              AND LOWER(depart.ville) = LOWER(:villeDepart)
              AND LOWER(arrivee.ville) = LOWER(:villeArrivee)
              AND depart.ordre < arrivee.ordre
              AND t.dateDepart >= :debutJour
              AND t.dateDepart < :finJour
              AND (arrivee.prixDepuisDepart - depart.prixDepuisDepart) <= :prixMax
              AND (t.placesProposees - COALESCE(
                    (SELECT SUM(r2.nombrePlaces) FROM Reservation r2
                     WHERE r2.trajet = t AND r2.statut = :statutReservation), 0L)) >= :nombrePlaces
            ORDER BY t.dateDepart
            """,
            countQuery = """
            SELECT COUNT(t)
            FROM Trajet t
            JOIN t.pointsArret depart
            JOIN t.pointsArret arrivee
            WHERE t.statut = :statutTrajet
              AND LOWER(depart.ville) = LOWER(:villeDepart)
              AND LOWER(arrivee.ville) = LOWER(:villeArrivee)
              AND depart.ordre < arrivee.ordre
              AND t.dateDepart >= :debutJour
              AND t.dateDepart < :finJour
              AND (arrivee.prixDepuisDepart - depart.prixDepuisDepart) <= :prixMax
              AND (t.placesProposees - COALESCE(
                    (SELECT SUM(r2.nombrePlaces) FROM Reservation r2
                     WHERE r2.trajet = t AND r2.statut = :statutReservation), 0L)) >= :nombrePlaces
            """)
    Page<TrajetRechercheResponse> rechercher(@Param("villeDepart") String villeDepart,
                                             @Param("villeArrivee") String villeArrivee,
                                             @Param("debutJour") LocalDateTime debutJour,
                                             @Param("finJour") LocalDateTime finJour,
                                             @Param("prixMax") BigDecimal prixMax,
                                             @Param("nombrePlaces") Integer nombrePlaces,
                                             @Param("statutTrajet") StatutTrajet statutTrajet,
                                             @Param("statutReservation") StatutReservation statutReservation,
                                             Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trajet t WHERE t.id = :id")
    Optional<Trajet> findByIdForUpdate(@Param("id") Long id);
}
