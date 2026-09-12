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
@Table(name = "roster_unit_mod_actuel",
       uniqueConstraints = @UniqueConstraint(columnNames = {"idMod", "ordreSecondaire"}),
       indexes = {
    	        @Index(name = "rmaidx_playerId", columnList = "playerId"),
    	        @Index(name = "rmaidx_definitionId", columnList = "definitionId"),
    	        @Index(name = "rmaidx_idSecondaire", columnList = "idSecondaire")
    			}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RosterUnitModActuel implements ModLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "roster_unit_mod_actuel_seq")
    @SequenceGenerator(name = "roster_unit_mod_actuel_seq", sequenceName = "roster_unit_mod_actuel_seq", allocationSize = BatchConstants.SEQUENCE_ALLOCATION_SIZE)
    private Long id;

    private String playerId;
    private String idUnit;
    private String idMod;
    private String definitionId;

    private String set;
    private String rarity;
    private String position;
    private Integer niveau;

    private Integer idPrimaire;
    private String primaire;
    private Long valeurPrimaire;

    private Integer idSecondaire;
    private String secondaire;
    private Long valeurSecondaire;
    private Integer ordreSecondaire;

    private Long idSync;
}