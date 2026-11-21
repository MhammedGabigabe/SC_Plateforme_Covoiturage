package ma.youcode.covoiturage.dto.response;

import ma.youcode.covoiturage.entity.enums.StatutReservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationConducteurResponse(
        Long id,
        Long passagerId,
        String passagerNom,
        String passagerPrenom,
        String villeMontee,
        String villeDescente,
        Integer nombrePlaces,
        BigDecimal montantTotal,
        LocalDateTime dateReservation,
        StatutReservation statut
) {
}
