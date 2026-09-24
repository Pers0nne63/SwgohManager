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
@Table(name = "unit_base_stat_definition",
       uniqueConstraints = @UniqueConstraint(columnNames = {"idUnit", "stat"}),
       indexes = {
     	        @Index(name = "ubsdefidx_idUnit", columnList = "idUnit")
     			}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnitBaseStatDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String idUnit;

    private Integer stat;
    private Long valeur;

    @CreationTimestamp
    @Column(name = "date_sync", updatable = false)
    private Instant dateSync;
}