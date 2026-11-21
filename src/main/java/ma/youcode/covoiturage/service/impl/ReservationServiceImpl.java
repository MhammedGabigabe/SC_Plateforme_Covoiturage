package ma.youcode.covoiturage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.youcode.covoiturage.dto.request.ReservationRequest;
import ma.youcode.covoiturage.dto.response.ReservationResponse;
import ma.youcode.covoiturage.entity.Passager;
import ma.youcode.covoiturage.entity.PointArret;
import ma.youcode.covoiturage.entity.Reservation;
import ma.youcode.covoiturage.entity.Trajet;
import ma.youcode.covoiturage.entity.enums.StatutReservation;
import ma.youcode.covoiturage.entity.enums.StatutTrajet;
import ma.youcode.covoiturage.exception.BusinessRuleException;
import ma.youcode.covoiturage.exception.InvalidRequestException;
import ma.youcode.covoiturage.exception.ResourceNotFoundException;
import ma.youcode.covoiturage.mapper.ReservationMapper;
import ma.youcode.covoiturage.repository.PassagerRepository;
import ma.youcode.covoiturage.repository.ReservationRepository;
import ma.youcode.covoiturage.repository.TrajetRepository;
import ma.youcode.covoiturage.service.ReservationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final TrajetRepository trajetRepository;
    private final PassagerRepository passagerRepository;
    private final ReservationMapper reservationMapper;

    @Override
    public ReservationResponse reserver(Long passagerId, ReservationRequest request) {
        Passager passager = passagerRepository.findById(passagerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passager introuvable avec l'id " + passagerId));

        Trajet trajet = trajetRepository.findByIdForUpdate(request.trajetId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trajet introuvable avec l'id " + request.trajetId()));

        if (trajet.getStatut() != StatutTrajet.PUBLIE) {
            throw new BusinessRuleException("Le trajet " + trajet.getId()
                    + " n'est pas ouvert à la réservation (statut " + trajet.getStatut() + ")");
        }

        PointArret pointMontee = trouverPoint(request.pointMonteeId(), trajet);
        PointArret pointDescente = trouverPoint(request.pointDescenteId(), trajet);

        if (pointMontee.getOrdre() >= pointDescente.getOrdre()) {
            throw new InvalidRequestException("Le point de montée (" + pointMontee.getVille()
                    + ") doit être avant le point de descente (" + pointDescente.getVille() + ")");
        }

        if (reservationRepository.existsByPassagerIdAndTrajetIdAndStatut(
                passagerId, trajet.getId(), StatutReservation.CONFIRMEE)) {
            throw new BusinessRuleException("Le passager " + passagerId
                    + " a déjà une réservation confirmée sur le trajet " + trajet.getId());
        }

        int placesRestantes = calculerPlacesRestantes(trajet);

        if (request.nombrePlaces() > placesRestantes) {
            throw new BusinessRuleException("Il ne reste que " + placesRestantes
                    + " place(s) sur le trajet " + trajet.getId());
        }

        BigDecimal prixSegment = pointDescente.getPrixDepuisDepart().subtract(pointMontee.getPrixDepuisDepart());
        BigDecimal montantTotal = prixSegment
                .multiply(BigDecimal.valueOf(request.nombrePlaces()))
                .setScale(2, RoundingMode.HALF_UP);

        Reservation reservation = new Reservation();
        reservation.setPassager(passager);
        reservation.setTrajet(trajet);
        reservation.setPointMontee(pointMontee);
        reservation.setPointDescente(pointDescente);
        reservation.setNombrePlaces(request.nombrePlaces());
        reservation.setMontantTotal(montantTotal);
        reservation.setDateReservation(LocalDateTime.now());
        reservation.setStatut(StatutReservation.CONFIRMEE);
        Reservation reservationEnregistree = reservationRepository.save(reservation);

        placesRestantes = placesRestantes - request.nombrePlaces();

        if (placesRestantes == 0) {
            trajet.setStatut(StatutTrajet.COMPLET);
            trajetRepository.save(trajet);
        }

        log.info("Réservation {} confirmée : {} place(s) de {} à {} sur le trajet {}, montant {} DH",
                reservationEnregistree.getId(), request.nombrePlaces(), pointMontee.getVille(),
                pointDescente.getVille(), trajet.getId(), montantTotal);

        return reservationMapper.toResponse(reservationEnregistree, placesRestantes);
    }

    private PointArret trouverPoint(Long pointId, Trajet trajet) {
        for (PointArret point : trajet.getPointsArret()) {
            if (point.getId().equals(pointId)) {
                return point;
            }
        }
        throw new InvalidRequestException("Le point d'arrêt " + pointId
                + " n'appartient pas au trajet " + trajet.getId());
    }

    private int calculerPlacesRestantes(Trajet trajet) {
        Long placesReservees = reservationRepository
                .compterPlacesReservees(trajet.getId(), StatutReservation.CONFIRMEE);
        return trajet.getPlacesProposees() - placesReservees.intValue();
    }
}