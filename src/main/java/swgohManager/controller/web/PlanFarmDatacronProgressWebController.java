package swgohManager.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import swgohManager.service.PlanFarmDatacronProgressService;

@Controller
@RequestMapping("/web/plan-farm-datacron/progression")
@RequiredArgsConstructor
public class PlanFarmDatacronProgressWebController {

    private final PlanFarmDatacronProgressService progressService;

    @GetMapping
    public String page(Model model) {
        model.addAttribute("sets", progressService.construire());
        return "plan-farm-datacron-progression";
    }

    @GetMapping("/datacron/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("detail", progressService.construireDetail(id));
        return "plan-farm-datacron-detail";
    }
}