package ma.youcode.covoiturage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Conducteur extends Utilisateur{

    @Column(nullable = false, unique = true)
    private String numeroPermis;
}
