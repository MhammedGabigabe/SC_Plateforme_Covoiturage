package ma.youcode.covoiturage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class PointArret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trajet_id", nullable = false)
    private Trajet trajet;

    @Column(nullable = false)
    private String ville;

    @Column(nullable = false)
    private Integer ordre;

    @Column(nullable = false)
    private LocalDateTime heurePassage;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixDepuisDepart;

}
