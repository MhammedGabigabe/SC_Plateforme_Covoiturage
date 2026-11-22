package ma.youcode.covoiturage.service.impl;

import ma.youcode.covoiturage.dto.request.PointArretRequest;
import ma.youcode.covoiturage.dto.request.TrajetRequest;
import ma.youcode.covoiturage.entity.*;
import ma.youcode.covoiturage.entity.enums.StatutReservation;
import ma.youcode.covoiturage.entity.enums.StatutTrajet;
import ma.youcode.covoiturage.exception.BusinessRuleException;
import ma.youcode.covoiturage.exception.ForbiddenException;
import ma.youcode.covoiturage.exception.InvalidRequestException;
import ma.youcode.covoiturage.exception.ResourceNotFoundException;
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @Test
    @DisplayName("publier : conducteur introuvable → 404")
    void publier_conducteurInconnu_leveResourceNotFound() {
        when(conducteurRepository.existsById(99L)).thenReturn(false);
        TrajetRequest request = new TrajetRequest(1L, 3, parcoursValide());

        assertThatThrownBy(() -> trajetService.publier(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Conducteur introuvable");
        verify(trajetRepository, never()).save(any(Trajet.class));
    }

    @Test
    @DisplayName("publier : véhicule introuvable → 404")
    void publier_vehiculeInconnu_leveResourceNotFound() {
        when(conducteurRepository.existsById(1L)).thenReturn(true);
        when(vehiculeRepository.findById(99L)).thenReturn(Optional.empty());
        TrajetRequest request = new TrajetRequest(99L, 3, parcoursValide());

        assertThatThrownBy(() -> trajetService.publier(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Véhicule introuvable");
        verify(trajetRepository, never()).save(any(Trajet.class));
    }

    @Test
    @DisplayName("publier : véhicule d'un autre conducteur → 403 (scénario 8)")
    void publier_vehiculeDunAutreConducteur_leveForbidden() {
        Vehicule vehiculeDeAutrui = creerVehicule(2L, 4, creerConducteur(2L));
        when(conducteurRepository.existsById(1L)).thenReturn(true);
        when(vehiculeRepository.findById(2L)).thenReturn(Optional.of(vehiculeDeAutrui));
        TrajetRequest request = new TrajetRequest(2L, 3, parcoursValide());

        assertThatThrownBy(() -> trajetService.publier(1L, request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("n'appartient pas au conducteur");
        verify(trajetRepository, never()).save(any(Trajet.class));
    }

    @Test
    @DisplayName("publier : 5 places pour un véhicule de capacité 4 → 400 (scénario 7)")
    void publier_placesSuperieuresALaCapacite_leveInvalidRequest() {
        preparerConducteurEtVehicule();
        TrajetRequest request = new TrajetRequest(1L, 5, parcoursValide());

        assertThatThrownBy(() -> trajetService.publier(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("4 places au maximum");
        verify(trajetRepository, never()).save(any(Trajet.class));
    }

    @Test
    @DisplayName("publier : heure de départ dans le passé → 400")
    void publier_departDansLePasse_leveInvalidRequest() {
        preparerConducteurEtVehicule();
        LocalDateTime passe = LocalDateTime.now().minusHours(1);
        List<PointArretRequest> points = List.of(
                point("Rabat", passe, "0"),
                point("Casablanca", passe.plusHours(1), "40"));
        TrajetRequest request = new TrajetRequest(1L, 3, points);

        assertThatThrownBy(() -> trajetService.publier(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("dans le futur");
        verify(trajetRepository, never()).save(any(Trajet.class));
    }

    @Test
    @DisplayName("publier : prix du premier point différent de 0 → 400")
    void publier_premierPrixNonNul_leveInvalidRequest() {
        preparerConducteurEtVehicule();
        LocalDateTime depart = LocalDateTime.now().plusDays(2);
        List<PointArretRequest> points = List.of(
                point("Rabat", depart, "10"),
                point("Casablanca", depart.plusHours(1), "50"));
        TrajetRequest request = new TrajetRequest(1L, 3, points);

        assertThatThrownBy(() -> trajetService.publier(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("égal à 0");
        verify(trajetRepository, never()).save(any(Trajet.class));
    }

    @Test
    @DisplayName("publier : une ville répétée sur le parcours → 400")
    void publier_villeRepetee_leveInvalidRequest() {
        preparerConducteurEtVehicule();
        LocalDateTime depart = LocalDateTime.now().plusDays(2);
        List<PointArretRequest> points = List.of(
                point("Rabat", depart, "0"),
                point("Casablanca", depart.plusHours(1), "40"),
                point("casablanca", depart.plusHours(2), "70"));
        TrajetRequest request = new TrajetRequest(1L, 3, points);

        assertThatThrownBy(() -> trajetService.publier(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("plusieurs fois");
        verify(trajetRepository, never()).save(any(Trajet.class));
    }

    @Test
    @DisplayName("publier : heures de passage non strictement croissantes → 400")
    void publier_heuresNonCroissantes_leveInvalidRequest() {
        preparerConducteurEtVehicule();
        LocalDateTime depart = LocalDateTime.now().plusDays(2);
        List<PointArretRequest> points = List.of(
                point("Rabat", depart, "0"),
                point("Casablanca", depart, "40"));
        TrajetRequest request = new TrajetRequest(1L, 3, points);

        assertThatThrownBy(() -> trajetService.publier(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("doit être après");
        verify(trajetRepository, never()).save(any(Trajet.class));
    }

    @Test
    @DisplayName("publier : prix non strictement croissants → 400")
    void publier_prixNonCroissants_leveInvalidRequest() {
        preparerConducteurEtVehicule();
        LocalDateTime depart = LocalDateTime.now().plusDays(2);
        List<PointArretRequest> points = List.of(
                point("Rabat", depart, "0"),
                point("Casablanca", depart.plusHours(1), "40"),
                point("Settat", depart.plusHours(2), "40"));
        TrajetRequest request = new TrajetRequest(1L, 3, points);

        assertThatThrownBy(() -> trajetService.publier(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("doit être supérieur");
        verify(trajetRepository, never()).save(any(Trajet.class));
    }

    @Test
    @DisplayName("consulter : trajet introuvable → 404")
    void consulter_trajetInconnu_leveResourceNotFound() {
        when(trajetRepository.findDetailById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trajetService.consulter(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Trajet introuvable");
    }

    @Test
    @DisplayName("terminer : avant l'heure de départ → 409 (scénario 9)")
    void terminer_avantHeureDepart_leveBusinessRule() {
        Trajet trajet = creerTrajet(1L, StatutTrajet.PUBLIE, LocalDateTime.now().plusHours(5), 1L);
        when(trajetRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trajet));

        assertThatThrownBy(() -> trajetService.terminer(1L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("après son heure de départ");
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.PUBLIE);
        verify(trajetRepository, never()).save(any(Trajet.class));
    }

    @Test
    @DisplayName("terminer : un trajet déjà TERMINE (statut final) → 409")
    void terminer_statutFinal_leveBusinessRule() {
        Trajet trajet = creerTrajet(1L, StatutTrajet.TERMINE, LocalDateTime.now().minusHours(3), 1L);
        when(trajetRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trajet));

        assertThatThrownBy(() -> trajetService.terminer(1L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("seuls les trajets PUBLIE ou COMPLET");
        verify(trajetRepository, never()).save(any(Trajet.class));
    }

    @Test
    @DisplayName("terminer : trajet d'un autre conducteur → 403")
    void terminer_autreConducteur_leveForbidden() {
        Trajet trajet = creerTrajet(1L, StatutTrajet.PUBLIE, LocalDateTime.now().minusHours(1), 2L);
        when(trajetRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trajet));

        assertThatThrownBy(() -> trajetService.terminer(1L, 1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("n'appartient pas au conducteur");
        verify(trajetRepository, never()).save(any(Trajet.class));
    }

    @Test
    @DisplayName("terminer : trajet introuvable → 404")
    void terminer_trajetInconnu_leveResourceNotFound() {
        when(trajetRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trajetService.terminer(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Trajet introuvable");
    }

    @Test
    @DisplayName("annuler : un trajet déjà ANNULE (statut final) → 409, aucune réservation modifiée")
    void annuler_statutFinal_leveBusinessRule() {
        Trajet trajet = creerTrajet(1L, StatutTrajet.ANNULE, LocalDateTime.now().plusDays(3), 1L);
        when(trajetRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trajet));

        assertThatThrownBy(() -> trajetService.annuler(1L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("seuls les trajets PUBLIE ou COMPLET");
        verify(reservationRepository, never()).saveAll(any());
        verify(trajetRepository, never()).save(any(Trajet.class));
    }

    @Test
    @DisplayName("annuler : trajet d'un autre conducteur → 403, aucune réservation modifiée")
    void annuler_autreConducteur_leveForbidden() {
        Trajet trajet = creerTrajet(1L, StatutTrajet.PUBLIE, LocalDateTime.now().plusDays(3), 2L);
        when(trajetRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(trajet));

        assertThatThrownBy(() -> trajetService.annuler(1L, 1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("n'appartient pas au conducteur");
        verify(reservationRepository, never()).saveAll(any());
        verify(trajetRepository, never()).save(any(Trajet.class));
    }

}