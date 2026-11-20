package ma.youcode.covoiturage.service;

import ma.youcode.covoiturage.dto.request.PassagerRequest;
import ma.youcode.covoiturage.dto.response.PassagerResponse;

import java.util.List;

public interface PassagerService {

    PassagerResponse creer(PassagerRequest request);

    List<PassagerResponse> lister();

    PassagerResponse consulter(Long id);

    PassagerResponse modifier(Long id, Long passagerConnecteId, PassagerRequest request);

    PassagerResponse rechercherParEmail(String email);
}
