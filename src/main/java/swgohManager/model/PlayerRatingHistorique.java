package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "player_rating_historique")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerRatingHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String playerId;

    private Integer skillRating;
    private String leagueId;
    private Integer divisionId;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant dateReleve;
}