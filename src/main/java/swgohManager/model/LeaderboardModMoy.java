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
@Table(name = "leaderboard_mod_moy", uniqueConstraints = @UniqueConstraint(columnNames = "baseId"),
		indexes = {
        // 1. Index simple sur une colonne
        @Index(name = "ModMoyidx_baseId", columnList = "baseId")
		}
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaderboardModMoy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String baseId;

    private Integer nbEchantillons;

    private Double speed, pSpeed, pOff, fOff, pSante, fSante, pProt, fProt, pDef, fDef;
    private Double pot, ten, cc, dc, critAvoid, acc;

    @CreationTimestamp
    @Column(name = "date_sync", updatable = false)
    private Instant dateSync;
}