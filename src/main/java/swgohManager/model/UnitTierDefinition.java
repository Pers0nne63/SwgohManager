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
@Table(name = "unit_tier_definition",
       uniqueConstraints = @UniqueConstraint(columnNames = {"idUnit", "gear", "stat"}),
       indexes = {
   	        @Index(name = "utdefidx_idUnit", columnList = "idUnit")
   			}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnitTierDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String idUnit;

    private Integer gear;
    private Integer stat;
    private Long valeur;

    @CreationTimestamp
    @Column(name = "date_sync", updatable = false)
    private Instant dateSync;
}