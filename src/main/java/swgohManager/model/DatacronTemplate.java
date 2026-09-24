package swgohManager.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @CreationTimestamp
    @Column(name = "date_sync", updatable = false)
    private LocalDateTime dateSync;
}