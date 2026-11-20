package ma.youcode.covoiturage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PassagerRequest (
        @Schema(example = "Ziani")
        @NotBlank(message = "Le nom est obligatoire")
        String nom,

        @Schema(example = "Hajar")
        @NotBlank(message = "Le prénom est obligatoire")
        String prenom,

        @Schema(example = "hajar.ziani@mail.com")
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email n'est pas valide")
        String email,

        @Schema(example = "0664000001")
        @NotBlank(message = "Le téléphone est obligatoire")
        String telephone
){
}
