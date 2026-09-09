package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "guildes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Guildes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String guildId;

    private String name;

    private Integer GP;
    
    private Integer memberCount;

    @UpdateTimestamp
    private Instant dateMiseAJour;
}