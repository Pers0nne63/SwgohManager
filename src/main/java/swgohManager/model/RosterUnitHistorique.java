package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roster_unit_historique")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RosterUnitHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerId;
    private String idUnit;
    private String definitionId;
    private Integer etoiles;
    private Integer niveau;
    private Integer gear;
    private Integer relic;

    private Long idSync;
}