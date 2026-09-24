package swgohManager.controller.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import swgohManager.controller.dto.OmicronDetailProjection;
import swgohManager.repository.RosterUnitProgressionRepository;

@RestController
@RequiredArgsConstructor
public class ActiviteRosterApiController {

    private final RosterUnitProgressionRepository progressionRepository;

    public record OmicronDetailVM(String libelle) {}

    @GetMapping("/activite-roster/omicron-detail")
    public List<OmicronDetailVM> omicronDetail(
            @RequestParam String playerId, @RequestParam String baseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate jour) {

        return progressionRepository.findOmicronsObtenusJour(playerId, baseId, jour).stream()
                .map(p -> new OmicronDetailVM(libelleSkill(p)))
                .toList();
    }

    private String libelleSkill(OmicronDetailProjection p) {
        if (p.getSkillNumero() == null) {
            return p.getSkillType();
        }
        return p.getSkillType() + String.format("%02d", p.getSkillNumero());
    }
}