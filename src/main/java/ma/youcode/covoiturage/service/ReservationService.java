package ma.youcode.covoiturage.service;

import ma.youcode.covoiturage.dto.request.ReservationRequest;
import ma.youcode.covoiturage.dto.response.ReservationResponse;
import ma.youcode.covoiturage.dto.response.ReservationResumeResponse;

import java.util.List;

public interface ReservationService {

    ReservationResponse reserver(Long passagerId, ReservationRequest request);

    void annuler(Long reservationId, Long passagerId);

    List<ReservationResumeResponse> consulterMesReservations(Long passagerId);
}
