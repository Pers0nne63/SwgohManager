package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "external_roster_unit_mod_actuel",
        indexes = { @Index(name = "extrumaidx_playerId", columnList = "playerId") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalRosterUnitModActuel implements ModLigne {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "external_roster_unit_mod_actuel_seq")
    @SequenceGenerator(name = "external_roster_unit_mod_actuel_seq", sequenceName = "external_roster_unit_mod_actuel_seq", allocationSize = 50)
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
    private Long valeurPrimaire;
    private Integer idSecondaire;
    private Long valeurSecondaire;
    private Integer ordreSecondaire;
}