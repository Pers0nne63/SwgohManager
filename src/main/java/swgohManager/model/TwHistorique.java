package swgohManager.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tw_historique",
       uniqueConstraints = @UniqueConstraint(columnNames = {"guildId", "opponentGuildId", "startTime", "endTime"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tw_historique_seq")
    @SequenceGenerator(name = "tw_historique_seq", sequenceName = "tw_historique_seq", allocationSize = 1)
    private Long id;

    private String guildId;
    private String territoryWarId; // conservé à titre informatif, plus utilisé comme clé

    private Instant startTime;
    private Instant endTime;

    private Long notreScore;
    private Long scoreAdversaire;
    private Long pgInscriteAdversaire;
    private Long pgInscriteNous;

    private String opponentGuildId;
    private String opponentGuildName;
    private Long opponentGuildGP;
}