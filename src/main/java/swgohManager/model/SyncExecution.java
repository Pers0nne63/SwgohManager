package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "sync_execution")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SyncExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idSync;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant dateSync;
}