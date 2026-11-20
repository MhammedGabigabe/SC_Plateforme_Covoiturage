package ma.youcode.covoiturage.service;

import ma.youcode.covoiturage.dto.request.TrajetRequest;
import ma.youcode.covoiturage.dto.response.TrajetResponse;

public interface TrajetService {

    TrajetResponse publier(Long conducteurId, TrajetRequest request);
}
