package swgohManager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "datacron_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatacronTemplate {

    @Id
    private String idTemplate; // ex: "datacron_set_33_base"

    private Integer setId;
    private Integer initialTiers;
    private String referenceTemplateId;
    private Integer maxRerolls;
    private Boolean allowReroll;
    private Boolean focused;
    private String focusedIcon;
    private String focusedPrefab;
}