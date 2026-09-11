package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

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
    @SequenceGenerator(name = "external_roster_unit_skill_actuel_seq", sequenceName = "external_roster_unit_skill_actuel_seq", allocationSize = 50)
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