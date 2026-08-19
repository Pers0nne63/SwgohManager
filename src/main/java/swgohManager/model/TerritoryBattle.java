package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "territory_battle", uniqueConstraints = @UniqueConstraint(columnNames = "instanceId"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TerritoryBattle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String guildId;

    @Column(nullable = false)
    private String instanceId;

    private String definitionId;
    private Instant startTime;
    private Instant endTime;
    private Integer totalStars;
}