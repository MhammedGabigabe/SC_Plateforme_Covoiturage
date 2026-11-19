package ma.youcode.covoiturage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ConducteurRequest(
        @Schema(example = "Mansouri")
        @NotBlank(message = "Le nom est obligatoire")
        String nom,

        @Schema(example = "Rachid")
        @NotBlank(message = "Le prénom est obligatoire")
        String prenom,

        @Schema(example = "rachid.mansouri@mail.com")
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email n'est pas valide")
        String email,

        @Schema(example = "0663000001")
        @NotBlank(message = "Le téléphone est obligatoire")
        String telephone,

        @Schema(example = "P-100099")
        @NotBlank(message = "Le numéro de permis est obligatoire")
        String numeroPermis
) {
}