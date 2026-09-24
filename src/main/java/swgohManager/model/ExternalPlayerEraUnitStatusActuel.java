package swgohManager.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    
    @CreationTimestamp
    @Column(name = "date_sync", updatable = false)
    private LocalDateTime dateSync;
}