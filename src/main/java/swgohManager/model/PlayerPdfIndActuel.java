package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_pdfind_actuel", uniqueConstraints = @UniqueConstraint(columnNames = "playerId"),
		indexes = {
        @Index(name = "pdfindidx_playerId", columnList = "playerId")
		})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerPdfIndActuel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String playerId;

    private Integer atteint;
    private Integer total;
    private Double pourcentage;
    private Long idSync;
}