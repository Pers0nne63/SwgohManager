package swgohManager.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @CreationTimestamp
    @Column(name = "date_sync", updatable = false)
    private Instant dateSync;
}