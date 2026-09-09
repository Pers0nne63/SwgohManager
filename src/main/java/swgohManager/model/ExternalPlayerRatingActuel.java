package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "external_player_rating_actuel", uniqueConstraints = @UniqueConstraint(columnNames = "playerId"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalPlayerRatingActuel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String playerId;

    private Integer skillRating;
    private String leagueId;
    private Integer divisionId;
}