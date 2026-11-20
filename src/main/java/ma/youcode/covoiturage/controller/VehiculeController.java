package ma.youcode.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.dto.request.VehiculeRequest;
import ma.youcode.covoiturage.dto.response.VehiculeResponse;
import ma.youcode.covoiturage.service.VehiculeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicules")
@RequiredArgsConstructor
@Tag(name = "Véhicules", description = "Gestion des véhicules des conducteurs")
public class VehiculeController {

    private final VehiculeService vehiculeService;

    @Operation(summary = "Ajouter un véhicule",
            description = "Le véhicule est rattaché au conducteur de l'en-tête X-Conducteur-Id. "
                    + "L'immatriculation doit être unique et la capacité comprise entre 1 et 8.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Véhicule ajouté"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Conducteur introuvable"),
            @ApiResponse(responseCode = "409", description = "Immatriculation déjà utilisée")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehiculeResponse ajouter(
            @Parameter(description = "Identifiant du conducteur connecté", example = "1")
            @RequestHeader("X-Conducteur-Id") Long conducteurId,
            @Valid @RequestBody VehiculeRequest request) {
        return vehiculeService.ajouter(conducteurId, request);
    }

    @Operation(summary = "Lister les véhicules d'un conducteur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des véhicules"),
            @ApiResponse(responseCode = "404", description = "Conducteur introuvable")
    })
    @GetMapping
    public List<VehiculeResponse> listerParConducteur(
            @Parameter(description = "Identifiant du conducteur", example = "1")
            @RequestParam Long conducteurId) {
        return vehiculeService.listerParConducteur(conducteurId);
    }

    @Operation(summary = "Consulter un véhicule")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Véhicule trouvé"),
            @ApiResponse(responseCode = "404", description = "Véhicule introuvable")
    })
    @GetMapping("/{id}")
    public VehiculeResponse consulter(@PathVariable Long id) {
        return vehiculeService.consulter(id);
    }

    @Operation(summary = "Modifier un véhicule",
            description = "Un conducteur ne peut modifier que ses propres véhicules.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Véhicule modifié"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "403", description = "Véhicule d'un autre conducteur"),
            @ApiResponse(responseCode = "404", description = "Véhicule introuvable"),
            @ApiResponse(responseCode = "409", description = "Immatriculation déjà utilisée")
    })
    @PutMapping("/{id}")
    public VehiculeResponse modifier(
            @PathVariable Long id,
            @Parameter(description = "Identifiant du conducteur connecté", example = "1")
            @RequestHeader("X-Conducteur-Id") Long conducteurId,
            @Valid @RequestBody VehiculeRequest request) {
        return vehiculeService.modifier(id, conducteurId, request);
    }

    @Operation(summary = "Supprimer un véhicule",
            description = "Impossible si le véhicule est associé à un trajet.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Véhicule supprimé"),
            @ApiResponse(responseCode = "403", description = "Véhicule d'un autre conducteur"),
            @ApiResponse(responseCode = "404", description = "Véhicule introuvable"),
            @ApiResponse(responseCode = "409", description = "Véhicule associé à un trajet")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(
            @PathVariable Long id,
            @Parameter(description = "Identifiant du conducteur connecté", example = "1")
            @RequestHeader("X-Conducteur-Id") Long conducteurId) {
        vehiculeService.supprimer(id, conducteurId);
    }
}

