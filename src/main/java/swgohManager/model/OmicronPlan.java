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
@Table(name = "omicron_plan",
		indexes = {
        // 1. Index simple sur une colonne
        @Index(name = "omiidx_baseId", columnList = "baseId"),
        @Index(name = "omiidx_idSkill", columnList = "idSkill")
		})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OmicronPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String baseId;

    @Column(nullable = false)
    private String idSkill;

    private Integer priorite;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private Instant dateCreation;
}