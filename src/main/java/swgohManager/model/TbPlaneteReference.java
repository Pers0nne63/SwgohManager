package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_planete_reference", uniqueConstraints = @UniqueConstraint(columnNames = "planeteId"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TbPlaneteReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer planeteId;

    private Integer phase;
    private Integer conflict;
    private Boolean bonus;
    private String planeteName;

    private Integer toonStrikeClassic;
    private Integer toonStrikeSpecial;
    private Integer shipStrike;
    private Integer vagues;
    private Integer ms;

    private Long winToonClassic;
    private Long winToonSpecial;
    private Long winShip;
    private Long gpCombat;
}