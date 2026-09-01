package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_datacron_affix_actuel")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerDatacronAffixActuel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerId;
    private String idDatacron;
    private Integer ordre;

    private String tag;
    private String targetRule;
    private String abilityId;
    private Integer statType;
    private Long statValue;
    private Integer requiredUnitTier;
    private Integer requiredRelicTier;
    private String scopeIcon;
}