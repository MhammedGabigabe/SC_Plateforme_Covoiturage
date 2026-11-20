package ma.youcode.covoiturage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehiculeRequest(
        @Schema(example = "Renault")
        @NotBlank(message = "La marque est obligatoire")
        String marque,

        @Schema(example = "Megane")
        @NotBlank(message = "Le modèle est obligatoire")
        String modele,

        @Schema(example = "67890-C-5")
        @NotBlank(message = "L'immatriculation est obligatoire")
        String immatriculation,

        @Schema(example = "Vert")
        @NotBlank(message = "La couleur est obligatoire")
        String couleur,

        @Schema(example = "4")
        @NotNull(message = "La capacité est obligatoire")
        @Min(value = 1, message = "La capacité doit être au minimum de 1 place")
        @Max(value = 8, message = "La capacité doit être au maximum de 8 places")
        Integer capacite
) {
}
