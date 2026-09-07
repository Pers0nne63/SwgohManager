package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "external_roster_unit_actuel", uniqueConstraints = @UniqueConstraint(columnNames = "idUnit"),
        indexes = {
                @Index(name = "extruactidx_playerId", columnList = "playerId"),
                @Index(name = "extruactidx_definitionId", columnList = "definitionId")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalRosterUnitActuel {
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
}