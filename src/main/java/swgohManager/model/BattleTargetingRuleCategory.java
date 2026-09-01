package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "battle_targeting_rule_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleTargetingRuleCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String battleTargetingRuleId;
    private String categoryId;
}