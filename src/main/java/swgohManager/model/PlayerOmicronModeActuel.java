package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_omicron_mode_actuel",
       uniqueConstraints = @UniqueConstraint(columnNames = {"playerId", "omicronMode"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerOmicronModeActuel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerId;
    private String omicronMode;
    private Integer nbOmicron;
    private Long idSync;
}