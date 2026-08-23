package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "skill_definition", uniqueConstraints = @UniqueConstraint(columnNames = "idSkill"),
indexes = {
        @Index(name = "skdefidx_idSkill", columnList = "idSkill")
		})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SkillDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String idSkill;

    private Boolean skillZeta;
    private Integer tierZetaRequis;
    private String omicronMode;
    private Boolean skillOmicron;
    private Integer tierOmicronRequis;

    private String gameVersion;

    @UpdateTimestamp
    private Instant dateMiseAJour;
}