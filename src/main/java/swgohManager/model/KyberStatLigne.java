package swgohManager.model;

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
@Table(name = "kyber_stat_ligne")
@Getter @Setter @NoArgsConstructor
public class KyberStatLigne {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kyber_stat_ligne_seq")
    @SequenceGenerator(name = "kyber_stat_ligne_seq", sequenceName = "kyber_stat_ligne_seq", allocationSize = 50)
    private Long id;
    private Long snapshotId;
    private String type;
    private Integer cle;
    private Integer nbJoueurs;
    private Long pgMoyenne;
}