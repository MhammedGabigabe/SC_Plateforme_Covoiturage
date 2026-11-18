package ma.youcode.covoiturage.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Passager extends Utilisateur{

    @OneToMany(mappedBy = "passager")
    private List<Reservation> reservations = new ArrayList<>();
}
