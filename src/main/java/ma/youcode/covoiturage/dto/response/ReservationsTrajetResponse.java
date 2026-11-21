package ma.youcode.covoiturage.dto.response;

import ma.youcode.covoiturage.entity.enums.StatutTrajet;

import java.util.List;

public record ReservationsTrajetResponse(
        Long trajetId,
        String villeDepart,
        String villeArrivee,
        Integer placesProposees,
        Integer placesRestantes,
        StatutTrajet statutTrajet,
        List<ReservationConducteurResponse> reservations
) {
}
