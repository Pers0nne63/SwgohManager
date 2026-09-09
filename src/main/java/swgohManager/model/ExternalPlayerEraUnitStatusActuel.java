package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "external_player_era_unit_status_actuel",
       uniqueConstraints = @UniqueConstraint(columnNames = {"playerId", "unitBaseId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalPlayerEraUnitStatusActuel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerId;
    private String unitBaseId;
    private Integer eraLevel;
}