package ma.youcode.covoiturage.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TrajetRequest(

        @NotNull(message = "Le véhicule est obligatoire")
        Long vehiculeId,

        @NotNull(message = "Le nombre de places proposées est obligatoire")
        @Min(value = 1, message = "Il faut proposer au moins 1 place")
        Integer placesProposees,

        @NotNull(message = "Le parcours est obligatoire")
        @Size(min = 2, message = "Un trajet doit avoir au moins 2 points d'arrêt")
        @Valid
        List<PointArretRequest> points
) {
}
