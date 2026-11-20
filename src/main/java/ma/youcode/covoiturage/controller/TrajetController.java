package ma.youcode.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.dto.request.TrajetRequest;
import ma.youcode.covoiturage.dto.response.TrajetResponse;
import ma.youcode.covoiturage.service.TrajetService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trajets")
@RequiredArgsConstructor
@Tag(name = "Trajets", description = "Publication, consultation et recherche des trajets")
public class TrajetController {

    private final TrajetService trajetService;

    @Operation(summary = "Publier un trajet avec des points d'arrêt",
            description = "Le premier point est le départ (prix 0), le dernier est l'arrivée. "
                    + "Les heures et les prix doivent être strictement croissants et chaque ville apparaît une seule fois.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trajet publié"),
            @ApiResponse(responseCode = "400", description = "Parcours invalide ou trop de places proposées"),
            @ApiResponse(responseCode = "403", description = "Véhicule d'un autre conducteur"),
            @ApiResponse(responseCode = "404", description = "Conducteur ou véhicule introuvable")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrajetResponse publier(
            @Parameter(description = "Identifiant du conducteur connecté", example = "1")
            @RequestHeader("X-Conducteur-Id") Long conducteurId,
            @Valid @RequestBody TrajetRequest request) {
        return trajetService.publier(conducteurId, request);
    }
}