package ma.youcode.covoiturage.service.impl;

import ma.youcode.covoiturage.dto.request.ReservationRequest;
import ma.youcode.covoiturage.dto.response.ReservationResumeResponse;
import ma.youcode.covoiturage.entity.*;
import ma.youcode.covoiturage.entity.enums.StatutReservation;
import ma.youcode.covoiturage.entity.enums.StatutTrajet;
import ma.youcode.covoiturage.mapper.ReservationMapper;
import ma.youcode.covoiturage.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private TrajetRepository trajetRepository;
    @Mock
    private PassagerRepository passagerRepository;
    @Mock
    private ReservationMapper reservationMapper;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Passager creerPassager(Long id) {
        Passager passager = new Passager();
        passager.setId(id);
        return passager;
    }

    private PointArret creerPoint(Long id, String ville, int ordre, String prix) {
        PointArret point = new PointArret();
        point.setId(id);
        point.setVille(ville);
        point.setOrdre(ordre);
        point.setPrixDepuisDepart(new BigDecimal(prix));
        return point;
    }

    private Trajet creerTrajetDeHamid(StatutTrajet statut, int placesProposees) {
        Trajet trajet = new Trajet();
        trajet.setId(1L);
        trajet.setStatut(statut);
        trajet.setPlacesProposees(placesProposees);
        trajet.setDateDepart(LocalDateTime.now().plusDays(5));
        trajet.getPointsArret().add(creerPoint(1L, "Rabat", 0, "0"));
        trajet.getPointsArret().add(creerPoint(2L, "Casablanca", 1, "40"));
        trajet.getPointsArret().add(creerPoint(3L, "Settat", 2, "70"));
        trajet.getPointsArret().add(creerPoint(4L, "Marrakech", 3, "120"));
        return trajet;
    }

    private Reservation creerReservation(Long id, Passager passager, Trajet trajet, StatutReservation statut) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setPassager(passager);
        reservation.setTrajet(trajet);
        reservation.setStatut(statut);
        return reservation;
    }

    private void simulerPassagerEtTrajet(Passager passager, Trajet trajet) {
        when(passagerRepository.findById(passager.getId())).thenReturn(Optional.of(passager));
        when(trajetRepository.findByIdForUpdate(trajet.getId())).thenReturn(Optional.of(trajet));
    }

    @Test
    @DisplayName("reserver : 1 place de Settat à Marrakech coûte 50 DH et laisse 2 places (scénario 1)")
    void reserver_unePlaceSurUnSegment_calculeLeMontantEtLaisseLeTrajetPublie() {
        Passager salma = creerPassager(1L);
        Trajet trajet = creerTrajetDeHamid(StatutTrajet.PUBLIE, 3);
        simulerPassagerEtTrajet(salma, trajet);
        when(reservationRepository.compterPlacesReservees(1L, StatutReservation.CONFIRMEE)).thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.reserver(1L, new ReservationRequest(1L, 3L, 4L, 1));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        Reservation reservation = captor.getValue();

        assertThat(reservation.getMontantTotal()).isEqualTo(new BigDecimal("50.00"));
        assertThat(reservation.getNombrePlaces()).isEqualTo(1);
        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.CONFIRMEE);
        assertThat(reservation.getPassager()).isSameAs(salma);
        assertThat(reservation.getTrajet()).isSameAs(trajet);
        assertThat(reservation.getPointMontee().getVille()).isEqualTo("Settat");
        assertThat(reservation.getPointDescente().getVille()).isEqualTo("Marrakech");
        assertThat(reservation.getDateReservation()).isNotNull();

        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.PUBLIE);
        verify(trajetRepository, never()).save(any(Trajet.class));
        verify(reservationMapper).toResponse(any(Reservation.class), eq(2));
    }

    @Test
    @DisplayName("reserver : 2 places de Casablanca à Marrakech coûtent 160 DH et rendent le trajet COMPLET (scénario 3)")
    void reserver_derniereplaces_rendLeTrajetComplet() {
        Passager karim = creerPassager(2L);
        Trajet trajet = creerTrajetDeHamid(StatutTrajet.PUBLIE, 3);
        simulerPassagerEtTrajet(karim, trajet);
        // 1 place est déjà réservée (Salma) : il en reste 2
        when(reservationRepository.compterPlacesReservees(1L, StatutReservation.CONFIRMEE)).thenReturn(1L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.reserver(2L, new ReservationRequest(1L, 2L, 4L, 2));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());

        assertThat(captor.getValue().getMontantTotal()).isEqualTo(new BigDecimal("160.00"));

        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.COMPLET);
        verify(trajetRepository).save(trajet);
        verify(reservationMapper).toResponse(any(Reservation.class), eq(0));
    }

    @Test
    @DisplayName("annuler : l'annulation d'un trajet COMPLET le repasse à PUBLIE (scénario 5)")
    void annuler_trajetComplet_repasseLeTrajetAPublie() {
        Passager karim = creerPassager(2L);
        Trajet trajet = creerTrajetDeHamid(StatutTrajet.COMPLET, 3);
        trajet.setDateDepart(LocalDateTime.now().plusDays(3));
        Reservation reservation = creerReservation(10L, karim, trajet, StatutReservation.CONFIRMEE);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(trajetRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trajet));

        reservationService.annuler(10L, 2L);

        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.PUBLIE);
        verify(reservationRepository).save(reservation);
        verify(trajetRepository).save(trajet);
    }

    @Test
    @DisplayName("annuler : l'annulation sur un trajet PUBLIE ne change pas le statut du trajet")
    void annuler_trajetPublie_leTrajetResteSurPublie() {
        Passager salma = creerPassager(1L);
        Trajet trajet = creerTrajetDeHamid(StatutTrajet.PUBLIE, 3);
        Reservation reservation = creerReservation(10L, salma, trajet, StatutReservation.CONFIRMEE);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(trajetRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trajet));

        reservationService.annuler(10L, 1L);

        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.PUBLIE);
        verify(reservationRepository).save(reservation);
        verify(trajetRepository, never()).save(any(Trajet.class));
    }

    @Test
    @DisplayName("consulterMesReservations : renvoie toutes les réservations du passager")
    void consulterMesReservations_passagerExistant_renvoieSonHistorique() {
        Passager salma = creerPassager(1L);
        Trajet trajet = creerTrajetDeHamid(StatutTrajet.PUBLIE, 3);
        Reservation confirmee = creerReservation(10L, salma, trajet, StatutReservation.CONFIRMEE);
        Reservation annulee = creerReservation(11L, salma, trajet, StatutReservation.ANNULEE);
        when(passagerRepository.existsById(1L)).thenReturn(true);
        when(reservationRepository.listerParPassager(1L)).thenReturn(List.of(confirmee, annulee));

        List<ReservationResumeResponse> historique = reservationService.consulterMesReservations(1L);

        assertThat(historique).hasSize(2);
        verify(reservationMapper, times(2)).toResume(any(Reservation.class));
    }
}