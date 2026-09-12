package swgohManager.model;

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
@Table(name = "external_roster_unit_skill_actuel",
       indexes = {
           @Index(name = "erxidx_playerId", columnList = "playerId"),
           @Index(name = "erxidx_idUnit", columnList = "idUnit"),
           @Index(name = "erxidx_idSkill", columnList = "idSkill")
       }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalRosterUnitSkillActuel {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "external_roster_unit_skill_actuel_seq")
    @SequenceGenerator(name = "external_roster_unit_skill_actuel_seq", sequenceName = "external_roster_unit_skill_actuel_seq", allocationSize = BatchConstants.SEQUENCE_ALLOCATION_SIZE)
    private Long id;

    private String playerId;
    private String idUnit;
    private String idSkill;
    private Integer tier;
    private String type;
    private Integer numero;

    private Boolean skillZeta;
    private Boolean zetaApplied;
    private Boolean skillOmicron;
    private Boolean omicronApplied;
}