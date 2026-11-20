package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.request.VehiculeRequest;
import ma.youcode.covoiturage.dto.response.VehiculeResponse;
import ma.youcode.covoiturage.entity.Vehicule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface VehiculeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "conducteur", ignore = true)
    Vehicule toEntity(VehiculeRequest request);

    @Mapping(source = "conducteur.id", target = "conducteurId")
    VehiculeResponse toResponse(Vehicule vehicule);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "conducteur", ignore = true)
    void updateEntity(VehiculeRequest request, @MappingTarget Vehicule vehicule);
}
