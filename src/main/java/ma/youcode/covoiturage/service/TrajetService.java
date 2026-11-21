package ma.youcode.covoiturage.service;

import ma.youcode.covoiturage.dto.request.TrajetRequest;
import ma.youcode.covoiturage.dto.response.ReservationsTrajetResponse;
import ma.youcode.covoiturage.dto.response.TrajetRechercheResponse;
import ma.youcode.covoiturage.dto.response.TrajetResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TrajetService {

    TrajetResponse publier(Long conducteurId, TrajetRequest request);

    TrajetResponse consulter(Long id);

    Page<TrajetRechercheResponse> rechercher(String villeDepart, String villeArrivee, LocalDate date,
                                             Integer nombrePlaces, BigDecimal prixMax, int page, int taille);

    ReservationsTrajetResponse consulterReservations(Long trajetId, Long conducteurId);

    void terminer(Long trajetId, Long conducteurId);
}
