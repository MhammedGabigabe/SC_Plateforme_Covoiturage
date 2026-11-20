package ma.youcode.covoiturage.service;

import ma.youcode.covoiturage.dto.request.VehiculeRequest;
import ma.youcode.covoiturage.dto.response.VehiculeResponse;

import java.util.List;

public interface VehiculeService {

    VehiculeResponse ajouter(Long conducteurId, VehiculeRequest request);

    VehiculeResponse modifier(Long id, Long conducteurId, VehiculeRequest request);

    VehiculeResponse consulter(Long id);

    List<VehiculeResponse> listerParConducteur(Long conducteurId);

    void supprimer(Long id, Long conducteurId);
}
