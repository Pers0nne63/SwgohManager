package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_pdfomicron_actuel", uniqueConstraints = @UniqueConstraint(columnNames = "playerId"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerPdfOmicronActuel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String playerId;
    private String priorite;
    private Integer atteint;
    private Integer total;
    private Double pourcentage;
    private Long idSync;
}