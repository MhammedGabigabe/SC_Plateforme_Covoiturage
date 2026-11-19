package ma.youcode.covoiturage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ma.youcode.covoiturage.entity.enums.StatutTrajet;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Trajet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicule_id", nullable = false)
    private Vehicule vehicule;

    @Column(nullable = false)
    private LocalDateTime dateDepart;

    @Column(nullable = false)
    private Integer placesProposees;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutTrajet statut;

    @OneToMany(mappedBy = "trajet", cascade = CascadeType.ALL)
    @OrderBy("ordre ASC")
    private List<PointArret> pointsArret = new ArrayList<>();

    public String getVilleDepart() {
        if (pointsArret.isEmpty()) {
            return null;
        }
        return pointsArret.get(0).getVille();
    }

    public String getVilleArrivee() {
        if (pointsArret.isEmpty()) {
            return null;
        }
        return pointsArret.get(pointsArret.size() - 1).getVille();
    }
}
