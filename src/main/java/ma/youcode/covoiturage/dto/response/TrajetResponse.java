package ma.youcode.covoiturage.dto.response;

import ma.youcode.covoiturage.entity.enums.StatutTrajet;

import java.time.LocalDateTime;
import java.util.List;

public record TrajetResponse(
        Long id,
        String villeDepart,
        String villeArrivee,
        LocalDateTime dateDepart,
        Integer placesProposees,
        Integer placesRestantes,
        StatutTrajet statut,
        Long conducteurId,
        String conducteurNom,
        String conducteurPrenom,
        Long vehiculeId,
        String vehiculeMarque,
        String vehiculeModele,
        List<PointArretResponse> points
) {
}
