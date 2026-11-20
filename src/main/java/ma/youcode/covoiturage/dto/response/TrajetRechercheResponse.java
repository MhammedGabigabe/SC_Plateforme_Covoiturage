package ma.youcode.covoiturage.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TrajetRechercheResponse(
        Long trajetId,
        String villeDepart,
        String villeArrivee,
        LocalDateTime heureDepart,
        LocalDateTime heureArrivee,
        BigDecimal prixSegment
) {
}
