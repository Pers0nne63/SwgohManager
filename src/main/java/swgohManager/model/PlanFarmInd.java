package swgohManager.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "plan_farm_ind")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanFarmInd {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private String playerId;

    @Column(name = "nom_unite")
    private String nomUnite;

    @Column(name = "base_id", nullable = false)
    private String baseId;

    @Column(name = "etoiles_cible")
    private Integer etoilesCible;

    @Column(name = "relic_cible")
    private Integer relicCible;

    @Column(name = "tag")
    private String tag;
    
    @Column(name = "date_ajout")
    private Instant dateAjout;
}