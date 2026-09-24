package swgohManager.model;

import java.time.Instant;

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
@Table(name = "stat_definition", uniqueConstraints = @UniqueConstraint(columnNames = "statId"),
indexes = {
        @Index(name = "stdefidx_statId", columnList = "statId")
		})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StatDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer statId;

    private String nameKey;
    private String descKey;
    private Boolean isDecimal;
    private String name;
    private String detailedName;
    private String libellé;
    private Boolean isStatq;

    @CreationTimestamp
    @Column(name = "date_sync", updatable = false)
    private Instant dateSync;
}