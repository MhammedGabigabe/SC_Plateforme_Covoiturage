package ma.youcode.covoiturage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.youcode.covoiturage.dto.request.PassagerRequest;
import ma.youcode.covoiturage.dto.response.PassagerResponse;
import ma.youcode.covoiturage.entity.Passager;
import ma.youcode.covoiturage.exception.BusinessRuleException;
import ma.youcode.covoiturage.exception.ForbiddenException;
import ma.youcode.covoiturage.exception.ResourceNotFoundException;
import ma.youcode.covoiturage.mapper.PassagerMapper;
import ma.youcode.covoiturage.repository.PassagerRepository;
import ma.youcode.covoiturage.service.PassagerService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PassagerServiceImpl implements PassagerService {

    private final PassagerRepository passagerRepository;
    private final PassagerMapper passagerMapper;

    @Override
    public PassagerResponse creer(PassagerRequest request) {
        if (passagerRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Un passager avec l'email " + request.email() + " existe déjà");
        }

        Passager passager = passagerMapper.toEntity(request);
        Passager passagerEnregistre = passagerRepository.save(passager);

        log.info("Passager {} créé", passagerEnregistre.getId());
        return passagerMapper.toResponse(passagerEnregistre);
    }

    @Override
    public List<PassagerResponse> lister() {
        return passagerRepository.findAll().stream()
                .map(passagerMapper::toResponse)
                .toList();
    }

    @Override
    public PassagerResponse consulter(Long id) {
        Passager passager = trouverPassager(id);
        return passagerMapper.toResponse(passager);
    }

    @Override
    public PassagerResponse modifier(Long id, Long passagerConnecteId, PassagerRequest request) {
        if (!id.equals(passagerConnecteId)) {
            throw new ForbiddenException("Un passager ne peut modifier que son propre profil");
        }

        Passager passager = trouverPassager(id);

        if (!passager.getEmail().equals(request.email())
                && passagerRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Un passager avec l'email " + request.email() + " existe déjà");
        }

        passagerMapper.updateEntity(request, passager);
        Passager passagerModifie = passagerRepository.save(passager);

        log.info("Passager {} modifié", id);
        return passagerMapper.toResponse(passagerModifie);
    }

    @Override
    public PassagerResponse rechercherParEmail(String email) {
        Passager passager = passagerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun passager avec l'email " + email));
        return passagerMapper.toResponse(passager);
    }

    private Passager trouverPassager(Long id) {
        return passagerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passager introuvable avec l'id " + id));
    }
}
