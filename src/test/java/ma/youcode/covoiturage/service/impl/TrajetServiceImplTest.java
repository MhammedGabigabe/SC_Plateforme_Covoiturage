package ma.youcode.covoiturage.service.impl;

import ma.youcode.covoiturage.dto.request.PointArretRequest;
import ma.youcode.covoiturage.dto.request.TrajetRequest;
import ma.youcode.covoiturage.entity.*;
import ma.youcode.covoiturage.entity.enums.StatutReservation;
import ma.youcode.covoiturage.entity.enums.StatutTrajet;
import ma.youcode.covoiturage.mapper.ReservationMapper;
import ma.youcode.covoiturage.mapper.TrajetMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrajetServiceImplTest {

    @Mock
    private TrajetRepository trajetRepository;
    @Mock
    private VehiculeRepository vehiculeRepository;
    @Mock
    private ConducteurRepository conducteurRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private TrajetMapper trajetMapper;
    @Mock
    private ReservationMapper reservationMapper;

    @InjectMocks
    private TrajetServiceImpl trajetService;

    private Conducteur creerConducteur(Long id) {
        Conducteur conducteur = new Conducteur();
        conducteur.setId(id);
        return conducteur;
    }

    private Vehicule creerVehicule(Long id, Integer capacite, Conducteur conducteur) {
        Vehicule vehicule = new Vehicule();
        vehicule.setId(id);
        vehicule.setCapacite(capacite);
        vehicule.setConducteur(conducteur);
        return vehicule;
    }

    private Trajet creerTrajet(Long id, StatutTrajet statut, LocalDateTime dateDepart, Long conducteurId) {
        Trajet trajet = new Trajet();
        trajet.setId(id);
        trajet.setVehicule(creerVehicule(1L, 4, creerConducteur(conducteurId)));
        trajet.setStatut(statut);
        trajet.setDateDepart(dateDepart);
        trajet.setPlacesProposees(3);
        return trajet;
    }

    private Reservation creerReservation(StatutReservation statut) {
        Reservation reservation = new Reservation();
        reservation.setStatut(statut);
        return reservation;
    }

    private PointArretRequest point(String ville, LocalDateTime heure, String prix) {
        return new PointArretRequest(ville, heure, new BigDecimal(prix));
    }

    private List<PointArretRequest> parcoursValide() {
        LocalDateTime depart = LocalDateTime.now().plusDays(5);
        return List.of(
                point("Rabat", depart, "0"),
                point("Casablanca", depart.plusMinutes(75), "40"),
                point("Settat", depart.plusMinutes(135), "70"),
                point("Marrakech", depart.plusMinutes(270), "120"));
    }

    private Vehicule preparerConducteurEtVehicule() {
        Vehicule vehicule = creerVehicule(1L, 4, creerConducteur(1L));
        when(conducteurRepository.existsById(1L)).thenReturn(true);
        when(vehiculeRepository.findById(1L)).thenReturn(Optional.of(vehicule));
        return vehicule;
    }

    @Test
    @DisplayName("publier : un trajet valide est enregistré PUBLIE avec ses points ordonnés")
    void publier_trajetValide_enregistreUnTrajetPublie() {
        Vehicule vehicule = preparerConducteurEtVehicule();
        List<PointArretRequest> points = parcoursValide();
        when(trajetRepository.save(any(Trajet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        trajetService.publier(1L, new TrajetRequest(1L, 3, points));

        ArgumentCaptor<Trajet> captor = ArgumentCaptor.forClass(Trajet.class);
        verify(trajetRepository).save(captor.capture());
        Trajet trajet = captor.getValue();

        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.PUBLIE);
        assertThat(trajet.getVehicule()).isSameAs(vehicule);
        assertThat(trajet.getPlacesProposees()).isEqualTo(3);
        assertThat(trajet.getDateDepart()).isEqualTo(points.get(0).heurePassage());
        assertThat(trajet.getPointsArret()).hasSize(4);
        assertThat(trajet.getPointsArret()).extracting(PointArret::getOrdre).containsExactly(0, 1, 2, 3);
        assertThat(trajet.getPointsArret()).extracting(PointArret::getVille)
                .containsExactly("Rabat", "Casablanca", "Settat", "Marrakech");
        assertThat(trajet.getVilleDepart()).isEqualTo("Rabat");
        assertThat(trajet.getVilleArrivee()).isEqualTo("Marrakech");
    }

    @Test
    @DisplayName("publier : les prix sont enregistrés avec 2 décimales")
    void publier_prixAvecPlusieursDecimales_arrondiADeuxDecimales() {
        preparerConducteurEtVehicule();
        LocalDateTime depart = LocalDateTime.now().plusDays(3);
        List<PointArretRequest> points = List.of(
                point("Rabat", depart, "0"),
                point("Casablanca", depart.plusHours(1), "12.345"));
        when(trajetRepository.save(any(Trajet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        trajetService.publier(1L, new TrajetRequest(1L, 2, points));

        ArgumentCaptor<Trajet> captor = ArgumentCaptor.forClass(Trajet.class);
        verify(trajetRepository).save(captor.capture());
        List<PointArret> pointsEnregistres = captor.getValue().getPointsArret();

        assertThat(pointsEnregistres.get(0).getPrixDepuisDepart()).isEqualTo(new BigDecimal("0.00"));
        assertThat(pointsEnregistres.get(1).getPrixDepuisDepart()).isEqualTo(new BigDecimal("12.35"));
    }

    @Test
    @DisplayName("consulter : les places restantes sont calculées à partir des réservations confirmées")
    void consulter_trajetExistant_calculeLesPlacesRestantes() {
        Trajet trajet = creerTrajet(1L, StatutTrajet.PUBLIE, LocalDateTime.now().plusDays(2), 1L);
        when(trajetRepository.findDetailById(1L)).thenReturn(Optional.of(trajet));
        when(reservationRepository.compterPlacesReservees(1L, StatutReservation.CONFIRMEE)).thenReturn(2L);

        trajetService.consulter(1L);

        verify(trajetMapper).toResponse(trajet, 1);
    }

    @Test
    @DisplayName("terminer : un trajet PUBLIE dont l'heure de départ est passée devient TERMINE")
    void terminer_apresHeureDepart_passeLeTrajetATermine() {
        Trajet trajet = creerTrajet(1L, StatutTrajet.PUBLIE, LocalDateTime.now().minusHours(1), 1L);
        when(trajetRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trajet));

        trajetService.terminer(1L, 1L);

        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.TERMINE);
        verify(trajetRepository).save(trajet);
    }

    @Test
    @DisplayName("annuler : le trajet et toutes ses réservations confirmées sont annulés ensemble")
    void annuler_trajetAvecReservations_annuleLeTrajetEtLesReservations() {
        Trajet trajet = creerTrajet(1L, StatutTrajet.COMPLET, LocalDateTime.now().plusDays(3), 1L);
        Reservation reservation1 = creerReservation(StatutReservation.CONFIRMEE);
        Reservation reservation2 = creerReservation(StatutReservation.CONFIRMEE);
        List<Reservation> reservations = List.of(reservation1, reservation2);
        when(trajetRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trajet));
        when(reservationRepository.findByTrajetIdAndStatut(1L, StatutReservation.CONFIRMEE))
                .thenReturn(reservations);

        trajetService.annuler(1L, 1L);

        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.ANNULE);
        assertThat(reservation1.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(reservation2.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        verify(reservationRepository).saveAll(reservations);
        verify(trajetRepository).save(trajet);
    }

}