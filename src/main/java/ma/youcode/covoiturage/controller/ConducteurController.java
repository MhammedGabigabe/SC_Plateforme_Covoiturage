package ma.youcode.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.dto.request.ConducteurRequest;
import ma.youcode.covoiturage.dto.response.ConducteurResponse;
import ma.youcode.covoiturage.service.ConducteurService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/conducteurs")
@RequiredArgsConstructor
@Tag(name = "Conducteurs", description = "Gestion des profils des conducteurs")
public class ConducteurController {

    private final ConducteurService conducteurService;

    @Operation(summary = "Créer un conducteur",
            description = "L'email et le numéro de permis doivent être uniques.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conducteur créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides (champ obligatoire manquant, email incorrect)"),
            @ApiResponse(responseCode = "409", description = "Email ou numéro de permis déjà utilisé")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConducteurResponse creer(@Valid @RequestBody ConducteurRequest request) {
        return conducteurService.creer(request);
    }

    @Operation(summary = "Lister les conducteurs")
    @ApiResponse(responseCode = "200", description = "Liste de tous les conducteurs")
    @GetMapping
    public List<ConducteurResponse> lister() {
        return conducteurService.lister();
    }

    @Operation(summary = "Rechercher un conducteur par nom ou par email",
            description = "Si l'email est renseigné, il est prioritaire. Sans critère, tous les conducteurs sont renvoyés.")
    @ApiResponse(responseCode = "200", description = "Conducteurs correspondant à la recherche")
    @GetMapping("/recherche")
    public List<ConducteurResponse> rechercher(
            @Parameter(description = "Nom (ou début de nom), sans tenir compte des majuscules")
            @RequestParam(required = false) String nom,
            @Parameter(description = "Email exact")
            @RequestParam(required = false) String email) {
        return conducteurService.rechercher(nom, email);
    }

    @Operation(summary = "Consulter un conducteur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conducteur trouvé"),
            @ApiResponse(responseCode = "404", description = "Conducteur introuvable")
    })
    @GetMapping("/{id}")
    public ConducteurResponse consulter(@PathVariable Long id) {
        return conducteurService.consulter(id);
    }

    @Operation(summary = "Modifier son profil",
            description = "Un conducteur ne peut modifier que son propre profil.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil modifié"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou en-tête X-Conducteur-Id manquant"),
            @ApiResponse(responseCode = "403", description = "Tentative de modifier le profil d'un autre conducteur"),
            @ApiResponse(responseCode = "404", description = "Conducteur introuvable"),
            @ApiResponse(responseCode = "409", description = "Email ou numéro de permis déjà utilisé")
    })
    @PutMapping("/{id}")
    public ConducteurResponse modifier(
            @PathVariable Long id,
            @Parameter(description = "Identifiant du conducteur connecté (doit être égal à l'id modifié)", example = "1")
            @RequestHeader("X-Conducteur-Id") Long conducteurId,
            @Valid @RequestBody ConducteurRequest request) {
        return conducteurService.modifier(id, conducteurId, request);
    }

}
