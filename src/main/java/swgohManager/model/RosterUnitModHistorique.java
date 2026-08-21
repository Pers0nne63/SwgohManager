package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roster_unit_mod_historique",
indexes = {
        @Index(name = "rmhidx_playerId", columnList = "playerId"),
        @Index(name = "rmhidx_definitionId", columnList = "definitionId"),
        @Index(name = "rmhidx_idSecondaire", columnList = "idSecondaire")
		})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RosterUnitModHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    private Long idSync;
}