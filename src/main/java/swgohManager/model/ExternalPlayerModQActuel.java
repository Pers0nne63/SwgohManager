package swgohManager.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "external_player_modq_actuel", uniqueConstraints = @UniqueConstraint(columnNames = "playerId"),
        indexes = { @Index(name = "extmodqidx_playerId", columnList = "playerId") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalPlayerModQActuel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String playerId;
    private Integer mod25Plus;
    private Integer mod20_24;
    private Integer mod15_19;
    private Integer mod10_14;
    private Double modQ;
    
    @CreationTimestamp
    @Column(name = "date_sync", updatable = false)
    private LocalDateTime dateSync;
}