package ma.youcode.covoiturage.service;

import ma.youcode.covoiturage.dto.request.ReservationRequest;
import ma.youcode.covoiturage.dto.response.ReservationResponse;

public interface ReservationService {

    ReservationResponse reserver(Long passagerId, ReservationRequest request);

}
