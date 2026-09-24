package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "guild_bilan_actuel")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GuildBilanActuel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant dateCalcul;
    private String guildId;
    private String guildNom;
    private Integer nbMembres;
    private Long galacticPowerTotal;
    private Integer etoilesBT;
    private Long scoreRaidTotal;

    private Double modQMoyen;
    private Long mod25Plus;
    private Long mod20A24;
    private Long mod15A19;

    private Double statQMoyen;

    private Long relic10;
    private Long relic9;
    private Long relic8;
    private Long relic6Et7;
    private Long relic0A5;
    private Long sansRelic;

    private Long nbFdtc;
    private Long nbDtc9;
    private Long nbOmicronTb;
    private Long nbOmicronTw;
    
    private Long twNombreAnalysees;
    private Double twPgInscriteMoyenne;
    private Double twScoreMoyen;
    private Double twScoreAdversaireMoyen;
    private Long twVictoires;
    private Long twDefaites;
    private Double twEcartMoyen;

    @Column(columnDefinition = "TEXT")
    private String statQParTeamJson;
    
}