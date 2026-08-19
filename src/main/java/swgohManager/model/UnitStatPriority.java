package swgohManager.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "unit_stat_priority")
@Data
@NoArgsConstructor
public class UnitStatPriority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_id", nullable = false, unique = true)
    private String baseId;
    private String team;

    @Column(name = "stat_id_1")
    private Integer statId1;

    @Column(name = "stat_id_2")
    private Integer statId2;

    @Column(name = "stat_id_3")
    private Integer statId3;

    @Column(name = "stat_id_4")
    private Integer statId4;
}