package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_datacron_actuel")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerDatacronActuel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerId;
    private String idDatacron;
    private Integer setId;
    private String templateId;
    private Boolean locked;
    private Integer rerollIndex;
    private Integer rerollCount;
    private Boolean focused;
}