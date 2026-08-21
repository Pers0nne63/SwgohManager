package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roster_unit_skill_historique",
indexes = {
	        @Index(name = "rshidx_playerId", columnList = "playerId"),
	        @Index(name = "rshidx_idUnit", columnList = "idUnit"),
	        @Index(name = "rshidx_idSkill", columnList = "idSkill")
			})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RosterUnitSkillHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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