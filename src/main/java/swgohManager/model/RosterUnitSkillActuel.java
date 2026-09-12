package swgohManager.model;

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
@Table(name = "roster_unit_skill_actuel",
       uniqueConstraints = @UniqueConstraint(columnNames = {"idUnit", "idSkill"}),
       indexes = {
   	        @Index(name = "rsaidx_playerId", columnList = "playerId"),
   	        @Index(name = "rsaidx_idUnit", columnList = "idUnit"),
   	        @Index(name = "rsaidx_idSkill", columnList = "idSkill")
   			}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RosterUnitSkillActuel {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "roster_unit_skill_actuel_seq")
    @SequenceGenerator(name = "roster_unit_skill_actuel_seq", sequenceName = "roster_unit_skill_actuel_seq", allocationSize = BatchConstants.SEQUENCE_ALLOCATION_SIZE)
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

    private Long idSync;
}