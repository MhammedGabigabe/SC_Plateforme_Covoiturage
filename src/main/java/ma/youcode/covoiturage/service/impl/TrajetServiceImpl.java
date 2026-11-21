package ma.youcode.covoiturage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.youcode.covoiturage.dto.request.PointArretRequest;
import ma.youcode.covoiturage.dto.request.TrajetRequest;
import ma.youcode.covoiturage.dto.response.ReservationConducteurResponse;
import ma.youcode.covoiturage.dto.response.ReservationsTrajetResponse;
import ma.youcode.covoiturage.dto.response.TrajetRechercheResponse;
import ma.youcode.covoiturage.dto.response.TrajetResponse;
import ma.youcode.covoiturage.entity.PointArret;
import ma.youcode.covoiturage.entity.Trajet;
import ma.youcode.covoiturage.entity.Vehicule;
import ma.youcode.covoiturage.entity.enums.StatutReservation;
import ma.youcode.covoiturage.entity.enums.StatutTrajet;
import ma.youcode.covoiturage.exception.ForbiddenException;
import ma.youcode.covoiturage.exception.InvalidRequestException;
import ma.youcode.covoiturage.exception.ResourceNotFoundException;
import ma.youcode.covoiturage.mapper.ReservationMapper;
import ma.youcode.covoiturage.mapper.TrajetMapper;
import ma.youcode.covoiturage.repository.ConducteurRepository;
import ma.youcode.covoiturage.repository.ReservationRepository;
import ma.youcode.covoiturage.repository.TrajetRepository;
import ma.youcode.covoiturage.repository.VehiculeRepository;
import ma.youcode.covoiturage.service.TrajetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TrajetServiceImpl implements TrajetService {

    private static final BigDecimal PRIX_MAXIMUM = new BigDecimal("99999999.99");

    private final TrajetRepository trajetRepository;
    private final VehiculeRepository vehiculeRepository;
    private final ConducteurRepository conducteurRepository;
    private final ReservationRepository reservationRepository;
    private final TrajetMapper trajetMapper;
    private final ReservationMapper reservationMapper;

    @Override
    public TrajetResponse publier(Long conducteurId, TrajetRequest request) {
        if (!conducteurRepository.existsById(conducteurId)) {
            throw new ResourceNotFoundException("Conducteur introuvable avec l'id " + conducteurId);
        }

        Vehicule vehicule = vehiculeRepository.findById(request.vehiculeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Véhicule introuvable avec l'id " + request.vehiculeId()));

       if (!vehicule.getConducteur().getId().equals(conducteurId)) {
            throw new ForbiddenException("Le véhicule " + vehicule.getId()
                    + " n'appartient pas au conducteur " + conducteurId);
        }

        if (request.placesProposees() > vehicule.getCapacite()) {
            throw new InvalidRequestException("Le nombre de places proposées ne peut pas dépasser "
                    + "la capacité du véhicule : " + vehicule.getCapacite() + " places au maximum");
        }

        List<PointArretRequest> points = request.points();
        verifierParcours(points);

        Trajet trajet = new Trajet();
        trajet.setVehicule(vehicule);
        trajet.setDateDepart(points.get(0).heurePassage());
        trajet.setPlacesProposees(request.placesProposees());
        trajet.setStatut(StatutTrajet.PUBLIE);

        for (int i = 0; i < points.size(); i++) {
            PointArretRequest pointRequest = points.get(i);

            PointArret point = new PointArret();
            point.setTrajet(trajet);
            point.setVille(pointRequest.ville().trim());
            point.setOrdre(i);
            point.setHeurePassage(pointRequest.heurePassage());
            point.setPrixDepuisDepart(pointRequest.prixDepuisDepart().setScale(2, RoundingMode.HALF_UP));
            trajet.getPointsArret().add(point);
        }

        Trajet trajetEnregistre = trajetRepository.save(trajet);

        log.info("Trajet {} publié par le conducteur {} : {} -> {}", trajetEnregistre.getId(),
                conducteurId, trajetEnregistre.getVilleDepart(), trajetEnregistre.getVilleArrivee());

        return trajetMapper.toResponse(trajetEnregistre, trajetEnregistre.getPlacesProposees());
    }

    @Override
    public TrajetResponse consulter(Long id) {
        Trajet trajet = trajetRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable avec l'id " + id));

        int placesRestantes = calculerPlacesRestantes(trajet);
        return trajetMapper.toResponse(trajet, placesRestantes);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationsTrajetResponse consulterReservations(Long trajetId, Long conducteurId) {
        Trajet trajet = trajetRepository.findDetailById(trajetId)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable avec l'id " + trajetId));

        if (!trajet.getVehicule().getConducteur().getId().equals(conducteurId)) {
            throw new ForbiddenException("Le trajet " + trajetId + " n'appartient pas au conducteur " + conducteurId);
        }

        List<ReservationConducteurResponse> reservations = reservationRepository.listerParTrajet(trajetId).stream()
                .map(reservationMapper::toConducteurResponse)
                .toList();

        int placesRestantes = calculerPlacesRestantes(trajet);

        return new ReservationsTrajetResponse(trajet.getId(), trajet.getVilleDepart(), trajet.getVilleArrivee(),
                trajet.getPlacesProposees(), placesRestantes, trajet.getStatut(), reservations);
    }

    @Override
    public Page<TrajetRechercheResponse> rechercher(String villeDepart, String villeArrivee, LocalDate date,
                                                    Integer nombrePlaces, BigDecimal prixMax,
                                                    int page, int taille) {
        if (villeDepart.trim().equalsIgnoreCase(villeArrivee.trim())) {
            throw new InvalidRequestException("La ville de départ et la ville d'arrivée doivent être différentes");
        }
        if (nombrePlaces < 1) {
            throw new InvalidRequestException("Le nombre de places recherchées doit être au minimum de 1");
        }
        if (page < 0 || taille < 1 || taille > 50) {
            throw new InvalidRequestException("La page doit être >= 0 et la taille comprise entre 1 et 50");
        }

        if (prixMax == null) {
            prixMax = PRIX_MAXIMUM;
        }

        LocalDateTime debutJour = date.atStartOfDay();
        LocalDateTime finJour = date.plusDays(1).atStartOfDay();

        Page<TrajetRechercheResponse> resultats = trajetRepository.rechercher(
                villeDepart.trim(), villeArrivee.trim(), debutJour, finJour, prixMax, nombrePlaces,
                StatutTrajet.PUBLIE, StatutReservation.CONFIRMEE, PageRequest.of(page, taille));

        log.info("Recherche {} -> {} le {} : {} résultat(s)", villeDepart.trim(), villeArrivee.trim(), date,
                resultats.getTotalElements());
        return resultats;
    }

    private int calculerPlacesRestantes(Trajet trajet) {
        Long placesReservees = reservationRepository
                .compterPlacesReservees(trajet.getId(), StatutReservation.CONFIRMEE);
        return trajet.getPlacesProposees() - placesReservees.intValue();
    }

    private void verifierParcours(List<PointArretRequest> points) {
        PointArretRequest premier = points.get(0);

        if (!premier.heurePassage().isAfter(LocalDateTime.now())) {
            throw new InvalidRequestException("L'heure de départ doit être dans le futur");
        }

        if (premier.prixDepuisDepart().compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidRequestException("Le prix du premier point (le départ) doit être égal à 0");
        }

        Set<String> villes = new HashSet<>();

        for (int i = 0; i < points.size(); i++) {
            PointArretRequest point = points.get(i);

            String ville = point.ville().trim().toLowerCase();
            if (!villes.add(ville)) {
                throw new InvalidRequestException("La ville " + point.ville().trim()
                        + " apparaît plusieurs fois sur le parcours");
            }

            if (i > 0) {
                PointArretRequest precedent = points.get(i - 1);

                if (!point.heurePassage().isAfter(precedent.heurePassage())) {
                    throw new InvalidRequestException("L'heure de passage à " + point.ville().trim()
                            + " doit être après celle de " + precedent.ville().trim());
                }

                if (point.prixDepuisDepart().compareTo(precedent.prixDepuisDepart()) <= 0) {
                    throw new InvalidRequestException("Le prix à " + point.ville().trim()
                            + " doit être supérieur à celui de " + precedent.ville().trim());
                }
            }
        }
    }
}