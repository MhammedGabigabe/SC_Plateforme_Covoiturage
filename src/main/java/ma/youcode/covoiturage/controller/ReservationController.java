package ma.youcode.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.dto.request.ReservationRequest;
import ma.youcode.covoiturage.dto.response.ReservationResponse;
import ma.youcode.covoiturage.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Réservations", description = "Réservation et annulation de places sur un trajet")
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "Réserver des places entre un point de montée et un point de descente",
            description = "Le montant total est (prix de descente - prix de montée) x nombre de places. "
                    + "Le trajet passe à COMPLET quand il ne reste plus de place. "
                    + "Les identifiants des points d'arrêt sont visibles dans GET /api/trajets/{id}.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Réservation confirmée"),
            @ApiResponse(responseCode = "400", description = "Données invalides, montée après la descente ou point hors du trajet"),
            @ApiResponse(responseCode = "404", description = "Passager ou trajet introuvable"),
            @ApiResponse(responseCode = "409", description = "Trajet non réservable (complet, terminé, annulé), plus assez de places ou réservation déjà confirmée")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserver(
            @Parameter(description = "Identifiant du passager connecté", example = "1")
            @RequestHeader("X-Passager-Id") Long passagerId,
            @Valid @RequestBody ReservationRequest request) {
        return reservationService.reserver(passagerId, request);
    }
}