package ma.youcode.covoiturage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(example = """
        {
          "vehiculeId": 1,
          "placesProposees": 3,
          "points": [
            {"ville": "Rabat", "heurePassage": "2026-12-20T17:00:00", "prixDepuisDepart": 0},
            {"ville": "Casablanca", "heurePassage": "2026-12-20T18:15:00", "prixDepuisDepart": 40},
            {"ville": "Settat", "heurePassage": "2026-12-20T19:15:00", "prixDepuisDepart": 70},
            {"ville": "Marrakech", "heurePassage": "2026-12-20T21:30:00", "prixDepuisDepart": 120}
          ]
        }""")
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
