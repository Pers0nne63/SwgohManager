package swgohManager.controller.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import swgohManager.model.TerritoryBattle;
import swgohManager.repository.TerritoryBattleRepository;
import swgohManager.service.TbAnalyseService;
import swgohManager.service.TbPlanService;

@Controller
@RequestMapping("/tb-plan")
@RequiredArgsConstructor
public class TbPlanWebController {

    private final TbPlanService tbPlanService;
    private final TerritoryBattleRepository territoryBattleRepository;
    private final TbAnalyseService tbAnalyseService;

    @GetMapping
    public String page(@RequestParam(defaultValue = "modeles") String tab,
                        @RequestParam(required = false) Long tbId,
                        Model model) {
        model.addAttribute("plans", tbPlanService.getAllPlansAvecRounds());
        model.addAttribute("libellesPlanetes", tbPlanService.getLibellesPlanetes());
        model.addAttribute("optionsLs", tbPlanService.getOptionsLs());
        model.addAttribute("optionsDs", tbPlanService.getOptionsDs());
        model.addAttribute("optionsMix", tbPlanService.getOptionsMix());
        model.addAttribute("optionZeffo", tbPlanService.getZeffo().orElse(null));
        model.addAttribute("optionMandalore", tbPlanService.getMandalore().orElse(null));
        model.addAttribute("rounds", List.of(1, 2, 3, 4, 5, 6));

        List<TerritoryBattle> territoryBattles = territoryBattleRepository.findAllByOrderByStartTimeDesc();
        model.addAttribute("territoryBattles", territoryBattles);
        model.addAttribute("activeTab", tab);
        model.addAttribute("tbIdSelectionnee", tbId);

        if (tbId != null) {
            var analyseJoueurs = tbAnalyseService.analyserTb(tbId);
            model.addAttribute("analyseJoueurs", analyseJoueurs);
            model.addAttribute("syntheseRounds", tbAnalyseService.calculerSyntheseRounds(analyseJoueurs));
        }

        return "tb-plan";
    }

    @PostMapping("/ajouter")
    public String ajouter(@RequestParam String nom,
                           @RequestParam Integer etoilesCibles,
                           @RequestParam Map<String, String> allParams) {

        List<TbPlanService.RoundInput> rounds = new ArrayList<>();
        for (int r = 1; r <= 6; r++) {
            Long ls = parseLong(allParams.get("round" + r + "_ls"));
            Long ds = parseLong(allParams.get("round" + r + "_ds"));
            Long mix = parseLong(allParams.get("round" + r + "_mix"));
            Long zeffo = parseLong(allParams.get("round" + r + "_zeffo"));
            Long mandalore = parseLong(allParams.get("round" + r + "_mandalore"));
            rounds.add(new TbPlanService.RoundInput(r, ls, ds, mix, zeffo, mandalore));
        }

        tbPlanService.creerPlan(nom, etoilesCibles, rounds);
        return "redirect:/tb-plan?tab=modeles";
    }

    @PostMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id) {
        tbPlanService.supprimerPlan(id);
        return "redirect:/tb-plan?tab=modeles";
    }

    @PostMapping("/assigner")
    public String assignerPlan(@RequestParam Long tbId, @RequestParam(required = false) Long planId) {
        territoryBattleRepository.findById(tbId).ifPresent(tb -> {
            tb.setPlanId(planId);
            territoryBattleRepository.save(tb);
        });
        return "redirect:/tb-plan?tab=affectations";
    }

    private Long parseLong(String v) {
        if (v == null || v.isBlank()) return null;
        try { return Long.parseLong(v); } catch (NumberFormatException e) { return null; }
    }
}