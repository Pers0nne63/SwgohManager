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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "farm_plan",
		indexes = {
        // 1. Index simple sur une colonne
        @Index(name = "pdfidx_baseId", columnList = "baseId")
		}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FarmPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String baseId;

    private Integer etoilesCible;
    private Integer relicCible;
    private String tag;
    
    @CreationTimestamp
    @Column(name = "date_sync", updatable = false)
    private Instant dateSync;
}