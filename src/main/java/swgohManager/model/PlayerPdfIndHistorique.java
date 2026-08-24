package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_pdfind_historique",
		indexes = {
        @Index(name = "pdfindhidx_playerId", columnList = "playerId")
		})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerPdfIndHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerId;
    private Integer atteint;
    private Integer total;
    private Double pourcentage;
    private Long idSync;
}