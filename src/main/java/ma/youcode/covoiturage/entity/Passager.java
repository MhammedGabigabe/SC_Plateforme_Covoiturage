package ma.youcode.covoiturage.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Passager extends Utilisateur{

    @OneToMany(mappedBy = "passager")
    private List<Reservation> reservations = new ArrayList<>();
}
