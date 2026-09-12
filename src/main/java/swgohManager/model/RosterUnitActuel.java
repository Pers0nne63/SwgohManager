package swgohManager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import swgohManager.config.BatchConstants;

@Entity
@Table(name = "roster_unit_actuel", uniqueConstraints = @UniqueConstraint(columnNames = "idUnit"),
indexes = {
        @Index(name = "ruactidx_playerId", columnList = "playerId"),
        @Index(name = "ruactidx_definitionId", columnList = "definitionId")
		})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RosterUnitActuel {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "roster_unit_actuel_seq")
    @SequenceGenerator(name = "roster_unit_actuel_seq", sequenceName = "roster_unit_actuel_seq", allocationSize = BatchConstants.SEQUENCE_ALLOCATION_SIZE)
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