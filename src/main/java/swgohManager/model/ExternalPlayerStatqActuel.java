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
@Table(name = "external_player_statq_actuel", uniqueConstraints = @UniqueConstraint(columnNames = "playerId"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalPlayerStatqActuel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String playerId;
    private Double statq;
    private Integer nbStats;
    
    @CreationTimestamp
    @Column(name = "date_sync", updatable = false)
    private LocalDateTime dateSync;
}