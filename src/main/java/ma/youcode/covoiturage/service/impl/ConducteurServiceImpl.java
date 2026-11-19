package ma.youcode.covoiturage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.youcode.covoiturage.dto.request.ConducteurRequest;
import ma.youcode.covoiturage.dto.response.ConducteurResponse;
import ma.youcode.covoiturage.entity.Conducteur;
import ma.youcode.covoiturage.exception.BusinessRuleException;
import ma.youcode.covoiturage.exception.ForbiddenException;
import ma.youcode.covoiturage.exception.ResourceNotFoundException;
import ma.youcode.covoiturage.mapper.ConducteurMapper;
import ma.youcode.covoiturage.repository.ConducteurRepository;
import ma.youcode.covoiturage.service.ConducteurService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConducteurServiceImpl implements ConducteurService {

    private final ConducteurRepository conducteurRepository;
    private final ConducteurMapper conducteurMapper;

    @Override
    public ConducteurResponse creer(ConducteurRequest request) {
        if (conducteurRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Un conducteur avec l'email " + request.email() + " existe déjà");
        }
        if (conducteurRepository.existsByNumeroPermis(request.numeroPermis())) {
            throw new BusinessRuleException("Un conducteur avec le numéro de permis "
                    + request.numeroPermis() + " existe déjà");
        }

        Conducteur conducteur = conducteurMapper.toEntity(request);
        Conducteur conducteurEnregistre = conducteurRepository.save(conducteur);

        log.info("Conducteur {} créé", conducteurEnregistre.getId());
        return conducteurMapper.toResponse(conducteurEnregistre);
    }

    @Override
    public List<ConducteurResponse> lister() {
        return conducteurRepository.findAll().stream()
                .map(conducteurMapper::toResponse)
                .toList();
    }

    @Override
    public ConducteurResponse consulter(Long id) {
        Conducteur conducteur = trouverConducteur(id);
        return conducteurMapper.toResponse(conducteur);
    }

    @Override
    public ConducteurResponse modifier(Long id, Long conducteurConnecteId, ConducteurRequest request) {
        if (!id.equals(conducteurConnecteId)) {
            throw new ForbiddenException("Un conducteur ne peut modifier que son propre profil");
        }

        Conducteur conducteur = trouverConducteur(id);

        if (!conducteur.getEmail().equals(request.email())
                && conducteurRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Un conducteur avec l'email " + request.email() + " existe déjà");
        }
        if (!conducteur.getNumeroPermis().equals(request.numeroPermis())
                && conducteurRepository.existsByNumeroPermis(request.numeroPermis())) {
            throw new BusinessRuleException("Un conducteur avec le numéro de permis "
                    + request.numeroPermis() + " existe déjà");
        }

        conducteurMapper.updateEntity(request, conducteur);
        Conducteur conducteurModifie = conducteurRepository.save(conducteur);

        log.info("Conducteur {} modifié", id);
        return conducteurMapper.toResponse(conducteurModifie);
    }

    @Override
    public List<ConducteurResponse> rechercher(String nom, String email) {
        if (email != null && !email.isBlank()) {
            return conducteurRepository.findByEmail(email).stream()
                    .map(conducteurMapper::toResponse)
                    .toList();
        }
        if (nom != null && !nom.isBlank()) {
            return conducteurRepository.findByNomContainingIgnoreCase(nom).stream()
                    .map(conducteurMapper::toResponse)
                    .toList();
        }
        return lister();
    }

    private Conducteur trouverConducteur(Long id) {
        return conducteurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conducteur introuvable avec l'id " + id));
    }
}