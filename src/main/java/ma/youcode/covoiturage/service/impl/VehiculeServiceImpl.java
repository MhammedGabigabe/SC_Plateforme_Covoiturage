package ma.youcode.covoiturage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.youcode.covoiturage.dto.request.VehiculeRequest;
import ma.youcode.covoiturage.dto.response.VehiculeResponse;
import ma.youcode.covoiturage.entity.Conducteur;
import ma.youcode.covoiturage.entity.Vehicule;
import ma.youcode.covoiturage.exception.BusinessRuleException;
import ma.youcode.covoiturage.exception.ForbiddenException;
import ma.youcode.covoiturage.exception.ResourceNotFoundException;
import ma.youcode.covoiturage.mapper.VehiculeMapper;
import ma.youcode.covoiturage.repository.ConducteurRepository;
import ma.youcode.covoiturage.repository.TrajetRepository;
import ma.youcode.covoiturage.repository.VehiculeRepository;
import ma.youcode.covoiturage.service.VehiculeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VehiculeServiceImpl implements VehiculeService {

    private final VehiculeRepository vehiculeRepository;
    private final ConducteurRepository conducteurRepository;
    private final TrajetRepository trajetRepository;
    private final VehiculeMapper vehiculeMapper;

    @Override
    public VehiculeResponse ajouter(Long conducteurId, VehiculeRequest request) {
        Conducteur conducteur = trouverConducteur(conducteurId);

        if (vehiculeRepository.existsByImmatriculation(request.immatriculation())) {
            throw new BusinessRuleException("Un véhicule avec l'immatriculation "
                    + request.immatriculation() + " existe déjà");
        }

        Vehicule vehicule = vehiculeMapper.toEntity(request);
        vehicule.setConducteur(conducteur);
        Vehicule vehiculeEnregistre = vehiculeRepository.save(vehicule);

        log.info("Véhicule {} ajouté par le conducteur {}", vehiculeEnregistre.getId(), conducteurId);
        return vehiculeMapper.toResponse(vehiculeEnregistre);
    }

    @Override
    public VehiculeResponse modifier(Long id, Long conducteurId, VehiculeRequest request) {
        Vehicule vehicule = trouverVehicule(id);
        verifierProprietaire(vehicule, conducteurId);

        if (!vehicule.getImmatriculation().equals(request.immatriculation())
                && vehiculeRepository.existsByImmatriculation(request.immatriculation())) {
            throw new BusinessRuleException("Un véhicule avec l'immatriculation "
                    + request.immatriculation() + " existe déjà");
        }

        vehiculeMapper.updateEntity(request, vehicule);
        Vehicule vehiculeModifie = vehiculeRepository.save(vehicule);

        log.info("Véhicule {} modifié", id);
        return vehiculeMapper.toResponse(vehiculeModifie);
    }

    @Override
    public VehiculeResponse consulter(Long id) {
        Vehicule vehicule = trouverVehicule(id);
        return vehiculeMapper.toResponse(vehicule);
    }

    @Override
    public List<VehiculeResponse> listerParConducteur(Long conducteurId) {
        trouverConducteur(conducteurId);

        return vehiculeRepository.findByConducteurId(conducteurId).stream()
                .map(vehiculeMapper::toResponse)
                .toList();
    }

    @Override
    public void supprimer(Long id, Long conducteurId) {
        Vehicule vehicule = trouverVehicule(id);
        verifierProprietaire(vehicule, conducteurId);

        if (trajetRepository.existsByVehiculeId(id)) {
            throw new BusinessRuleException("Le véhicule " + id
                    + " est associé à au moins un trajet et ne peut pas être supprimé");
        }

        vehiculeRepository.delete(vehicule);
        log.info("Véhicule {} supprimé", id);
    }

    private Vehicule trouverVehicule(Long id) {
        return vehiculeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule introuvable avec l'id " + id));
    }

    private Conducteur trouverConducteur(Long id) {
        return conducteurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conducteur introuvable avec l'id " + id));
    }

    private void verifierProprietaire(Vehicule vehicule, Long conducteurId) {
        if (!vehicule.getConducteur().getId().equals(conducteurId)) {
            throw new ForbiddenException("Le véhicule " + vehicule.getId()
                    + " n'appartient pas au conducteur " + conducteurId);
        }
    }
}
