package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category_definition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDefinition {
    @Id
    private String id;

    @Column(columnDefinition = "TEXT")
    private String descKey;

    @Column(columnDefinition = "TEXT")
    private String libelle;
}