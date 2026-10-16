package swgohManager.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kyber_stat_snapshot")
@Getter @Setter @NoArgsConstructor
public class KyberStatSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kyber_stat_snapshot_seq")
    @SequenceGenerator(name = "kyber_stat_snapshot_seq", sequenceName = "kyber_stat_snapshot_seq", allocationSize = 1)
    private Long id;
    private LocalDateTime dateSnapshot;
    private Integer nbJoueurs;
    private Integer nbBrackets;
    private Integer nbErreurs;
}