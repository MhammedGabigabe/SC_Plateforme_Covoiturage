package ma.youcode.covoiturage.service;

import ma.youcode.covoiturage.dto.request.ConducteurRequest;
import ma.youcode.covoiturage.dto.response.ConducteurResponse;

import java.util.List;

public interface ConducteurService {

    ConducteurResponse creer(ConducteurRequest request);

    List<ConducteurResponse> lister();

    ConducteurResponse consulter(Long id);

    ConducteurResponse modifier(Long id, Long conducteurConnecteId, ConducteurRequest request);

    List<ConducteurResponse> rechercher(String nom, String email);
}
