package ma.youcode.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.dto.request.TrajetRequest;
import ma.youcode.covoiturage.dto.response.TrajetRechercheResponse;
import ma.youcode.covoiturage.dto.response.TrajetResponse;
import ma.youcode.covoiturage.service.TrajetService;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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


    @Operation(summary = "Rechercher des trajets entre deux villes",
            description = "Les villes peuvent être n'importe quels points du parcours, à condition que la ville de départ "
                    + "soit avant la ville d'arrivée. Seuls les trajets PUBLIE avec assez de places restantes sont renvoyés. "
                    + "Le prix maximum s'applique au prix du segment recherché. "
                    + "Résultats paginés et triés par heure de départ du trajet.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de résultats (éventuellement vide)"),
            @ApiResponse(responseCode = "400", description = "Paramètre manquant ou invalide")
    })
    @GetMapping("/recherche")
    public PagedModel<TrajetRechercheResponse> rechercher(
            @Parameter(description = "Ville de départ", example = "Settat")
            @RequestParam String villeDepart,
            @Parameter(description = "Ville d'arrivée", example = "Marrakech")
            @RequestParam String villeArrivee,
            @Parameter(description = "Date du trajet (AAAA-MM-JJ)", example = "2026-11-23")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Nombre de places souhaitées")
            @RequestParam(defaultValue = "1") Integer nombrePlaces,
            @Parameter(description = "Prix maximum du segment (optionnel)")
            @RequestParam(required = false) BigDecimal prixMax,
            @Parameter(description = "Numéro de page (à partir de 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page (1 à 50)")
            @RequestParam(defaultValue = "10") int taille) {
        Page<TrajetRechercheResponse> resultats =
                trajetService.rechercher(villeDepart, villeArrivee, date, nombrePlaces, prixMax, page, taille);
        return new PagedModel<>(resultats);
    }

    @Operation(summary = "Consulter le détail d'un trajet",
            description = "Renvoie le parcours (points ordonnés), les places restantes (calculées) et le conducteur.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trajet trouvé"),
            @ApiResponse(responseCode = "404", description = "Trajet introuvable")
    })
    @GetMapping("/{id}")
    public TrajetResponse consulter(
            @Parameter(description = "Identifiant du trajet", example = "1")
            @PathVariable Long id) {
        return trajetService.consulter(id);
    }
}