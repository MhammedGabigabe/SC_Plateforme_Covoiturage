package ma.youcode.covoiturage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.youcode.covoiturage.dto.request.PointArretRequest;
import ma.youcode.covoiturage.dto.request.TrajetRequest;
import ma.youcode.covoiturage.dto.response.TrajetResponse;
import ma.youcode.covoiturage.entity.PointArret;
import ma.youcode.covoiturage.entity.Trajet;
import ma.youcode.covoiturage.entity.Vehicule;
import ma.youcode.covoiturage.entity.enums.StatutTrajet;
import ma.youcode.covoiturage.exception.ForbiddenException;
import ma.youcode.covoiturage.exception.InvalidRequestException;
import ma.youcode.covoiturage.exception.ResourceNotFoundException;
import ma.youcode.covoiturage.mapper.TrajetMapper;
import ma.youcode.covoiturage.repository.ConducteurRepository;
import ma.youcode.covoiturage.repository.TrajetRepository;
import ma.youcode.covoiturage.repository.VehiculeRepository;
import ma.youcode.covoiturage.service.TrajetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TrajetServiceImpl implements TrajetService {

    private final TrajetRepository trajetRepository;
    private final VehiculeRepository vehiculeRepository;
    private final ConducteurRepository conducteurRepository;
    private final TrajetMapper trajetMapper;

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