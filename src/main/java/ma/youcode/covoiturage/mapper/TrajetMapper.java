package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.response.PointArretResponse;
import ma.youcode.covoiturage.dto.response.TrajetResponse;
import ma.youcode.covoiturage.entity.PointArret;
import ma.youcode.covoiturage.entity.Trajet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface TrajetMapper {

    @Mapping(source = "trajet.id", target = "id")
    @Mapping(source = "trajet.villeDepart", target = "villeDepart")
    @Mapping(source = "trajet.villeArrivee", target = "villeArrivee")
    @Mapping(source = "trajet.dateDepart", target = "dateDepart")
    @Mapping(source = "trajet.placesProposees", target = "placesProposees")
    @Mapping(source = "placesRestantes", target = "placesRestantes")
    @Mapping(source = "trajet.statut", target = "statut")
    @Mapping(source = "trajet.vehicule.conducteur.id", target = "conducteurId")
    @Mapping(source = "trajet.vehicule.conducteur.nom", target = "conducteurNom")
    @Mapping(source = "trajet.vehicule.conducteur.prenom", target = "conducteurPrenom")
    @Mapping(source = "trajet.vehicule.id", target = "vehiculeId")
    @Mapping(source = "trajet.vehicule.marque", target = "vehiculeMarque")
    @Mapping(source = "trajet.vehicule.modele", target = "vehiculeModele")
    @Mapping(source = "trajet.pointsArret", target = "points")
    TrajetResponse toResponse(Trajet trajet, Integer placesRestantes);

    PointArretResponse toPointResponse(PointArret point);
}
