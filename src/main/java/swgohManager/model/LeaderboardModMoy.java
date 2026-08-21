package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

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
}