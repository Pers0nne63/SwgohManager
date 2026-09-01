package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_era_unit_status_actuel",
       uniqueConstraints = @UniqueConstraint(columnNames = {"playerId", "unitBaseId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerEraUnitStatusActuel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerId;
    private String unitBaseId;
    private Integer eraLevel;
}