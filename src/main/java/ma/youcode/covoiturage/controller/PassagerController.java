package ma.youcode.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.dto.request.PassagerRequest;
import ma.youcode.covoiturage.dto.response.PassagerResponse;
import ma.youcode.covoiturage.service.PassagerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passagers")
@RequiredArgsConstructor
@Tag(name = "Passagers", description = "Gestion des profils des passagers")
public class PassagerController {

    private final PassagerService passagerService;

    @Operation(summary = "Créer un passager", description = "L'email doit être unique.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Passager créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "Email déjà utilisé")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PassagerResponse creer(@Valid @RequestBody PassagerRequest request) {
        return passagerService.creer(request);
    }

    @Operation(summary = "Lister les passagers")
    @ApiResponse(responseCode = "200", description = "Liste de tous les passagers")
    @GetMapping
    public List<PassagerResponse> lister() {
        return passagerService.lister();
    }

    @Operation(summary = "Rechercher un passager par email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Passager trouvé"),
            @ApiResponse(responseCode = "400", description = "Paramètre email manquant"),
            @ApiResponse(responseCode = "404", description = "Aucun passager avec cet email")
    })
    @GetMapping("/recherche")
    public PassagerResponse rechercherParEmail(
            @Parameter(description = "Email exact du passager", example = "salma.bennani@mail.com")
            @RequestParam String email) {
        return passagerService.rechercherParEmail(email);
    }

    @Operation(summary = "Consulter un passager")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Passager trouvé"),
            @ApiResponse(responseCode = "404", description = "Passager introuvable")
    })
    @GetMapping("/{id}")
    public PassagerResponse consulter(@PathVariable Long id) {
        return passagerService.consulter(id);
    }

    @Operation(summary = "Modifier son profil",
            description = "Un passager ne peut modifier que son propre profil.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil modifié"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou en-tête X-Passager-Id manquant"),
            @ApiResponse(responseCode = "403", description = "Tentative de modifier le profil d'un autre passager"),
            @ApiResponse(responseCode = "404", description = "Passager introuvable"),
            @ApiResponse(responseCode = "409", description = "Email déjà utilisé")
    })
    @PutMapping("/{id}")
    public PassagerResponse modifier(
            @PathVariable Long id,
            @Parameter(description = "Identifiant du passager connecté (doit être égal à l'id modifié)", example = "1")
            @RequestHeader("X-Passager-Id") Long passagerId,
            @Valid @RequestBody PassagerRequest request) {
        return passagerService.modifier(id, passagerId, request);
    }
}
