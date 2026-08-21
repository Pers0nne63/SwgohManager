package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_pdfomicron_historique",
		indexes = {
        @Index(name = "pdfomihidx_playerId", columnList = "playerId")
		})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerPdfOmicronHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String playerId;
    private String priorite;
    private Integer atteint;
    private Integer total;
    private Double pourcentage;
    private Long idSync;
}