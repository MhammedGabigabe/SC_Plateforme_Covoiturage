package ma.youcode.covoiturage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReservationRequest(
        @Schema(example = "1")
        @NotNull(message = "Le trajet est obligatoire")
        Long trajetId,

        @Schema(example = "3")
        @NotNull(message = "Le point de montée est obligatoire")
        Long pointMonteeId,

        @Schema(example = "4")
        @NotNull(message = "Le point de descente est obligatoire")
        Long pointDescenteId,

        @Schema(example = "1")
        @NotNull(message = "Le nombre de places est obligatoire")
        @Min(value = 1, message = "Il faut réserver au moins 1 place")
        Integer nombrePlaces
) {
}
