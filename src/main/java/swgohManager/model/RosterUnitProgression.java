package swgohManager.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import swgohManager.config.BatchConstants;

@Entity
@Table(name = "roster_unit_progression", indexes = {
        @Index(name = "rupidx_playerId", columnList = "playerId"),
        @Index(name = "rupidx_idUnit", columnList = "idUnit"),
        @Index(name = "rupidx_dateConstat", columnList = "dateConstat")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RosterUnitProgression {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "roster_unit_progression_seq")
    @SequenceGenerator(name = "roster_unit_progression_seq", sequenceName = "roster_unit_progression_seq",
            allocationSize = BatchConstants.SEQUENCE_ALLOCATION_SIZE)
    private Long id;

    private String playerId;
    private String idUnit;
    private String definitionId;

    private Instant dateConstat;
    private Long idSync;

    private Boolean nouvelleUnite;

    private Integer etoilesAvant;
    private Integer etoilesApres;
    private Integer gearAvant;
    private Integer gearApres;
    private Integer relicAvant;
    private Integer relicApres;
}