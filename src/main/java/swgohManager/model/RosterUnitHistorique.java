package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roster_unit_historique", indexes = {
        @Index(name = "ruhistidx_playerId", columnList = "playerId"),
        @Index(name = "ruhistidx_definitionId", columnList = "definitionId"),
        @Index(name = "ruhistidx_idSync", columnList = "idSync")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RosterUnitHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerId;

    @Column(nullable = false)
    private String idUnit;

    private String definitionId;
    private Integer etoiles;
    private Integer niveau;
    private Integer gear;
    private Integer relic;

    private Long idSync;
}