package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "guild_comparison_result", uniqueConstraints = @UniqueConstraint(columnNames = "guildIdB"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GuildComparisonResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant dateComparaison;

    private String guildIdA;
    private String guildNomA;
    private Integer nbMembresA;
    private Long galacticPowerTotalA;
    private Integer etoilesBtA;
    private Long scoreRaidTotalA;
    private Double modQMoyenA;
    private Long mod25PlusA;
    private Long mod20A24A;
    private Long mod15A19A;
    private Double statQMoyenA;
    private Long relic10A;
    private Long relic9A;
    private Long relic8A;
    private Long relic6Et7A;
    private Long relic0A5A;
    private Long sansRelicA;
    private Long nbFdtcA;
    private Long nbDtc9A;
    private Long nbOmicronTbA;
    private Long nbOmicronTwA;

    private String guildIdB;
    private String guildNomB;
    private Integer nbMembresB;
    private Long galacticPowerTotalB;
    private Integer etoilesBtB;
    private Long scoreRaidTotalB;
    private Double modQMoyenB;
    private Long mod25PlusB;
    private Long mod20A24B;
    private Long mod15A19B;
    private Double statQMoyenB;
    private Long relic10B;
    private Long relic9B;
    private Long relic8B;
    private Long relic6Et7B;
    private Long relic0A5B;
    private Long sansRelicB;
    private Long nbFdtcB;
    private Long nbDtc9B;
    private Long nbOmicronTbB;
    private Long nbOmicronTwB;

    @Column(columnDefinition = "TEXT")
    private String statQParTeamJsonA;
    @Column(columnDefinition = "TEXT")
    private String statQParTeamJsonB;
}