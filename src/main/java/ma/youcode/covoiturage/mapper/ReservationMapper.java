package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.response.ReservationConducteurResponse;
import ma.youcode.covoiturage.dto.response.ReservationResponse;
import ma.youcode.covoiturage.dto.response.ReservationResumeResponse;
import ma.youcode.covoiturage.entity.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ReservationMapper {

    @Mapping(source = "reservation.id", target = "id")
    @Mapping(source = "reservation.passager.id", target = "passagerId")
    @Mapping(source = "reservation.trajet.id", target = "trajetId")
    @Mapping(source = "reservation.pointMontee.ville", target = "villeMontee")
    @Mapping(source = "reservation.pointDescente.ville", target = "villeDescente")
    @Mapping(source = "reservation.nombrePlaces", target = "nombrePlaces")
    @Mapping(source = "reservation.montantTotal", target = "montantTotal")
    @Mapping(source = "reservation.dateReservation", target = "dateReservation")
    @Mapping(source = "reservation.statut", target = "statut")
    @Mapping(source = "placesRestantes", target = "placesRestantes")
    @Mapping(source = "reservation.trajet.statut", target = "statutTrajet")
    ReservationResponse toResponse(Reservation reservation, Integer placesRestantes);

    @Mapping(source = "trajet.id", target = "trajetId")
    @Mapping(source = "pointMontee.ville", target = "villeMontee")
    @Mapping(source = "pointDescente.ville", target = "villeDescente")
    @Mapping(source = "trajet.dateDepart", target = "dateDepart")
    @Mapping(source = "trajet.statut", target = "statutTrajet")
    ReservationResumeResponse toResume(Reservation reservation);

    @Mapping(source = "passager.id", target = "passagerId")
    @Mapping(source = "passager.nom", target = "passagerNom")
    @Mapping(source = "passager.prenom", target = "passagerPrenom")
    @Mapping(source = "pointMontee.ville", target = "villeMontee")
    @Mapping(source = "pointDescente.ville", target = "villeDescente")
    ReservationConducteurResponse toConducteurResponse(Reservation reservation);
}