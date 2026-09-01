package swgohManager.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import swgohManager.service.GuildEraUnitService;

import java.util.Map;

@Controller
@RequestMapping("/guilde/era-units")
@RequiredArgsConstructor
public class GuildEraUnitWebController {

    private final GuildEraUnitService guildEraUnitService;

    @GetMapping
    public String afficherEraUnits(Model model) {
        Map<String, Object> data = guildEraUnitService.getGuildeEraData();
        
        model.addAttribute("columns", data.get("columns"));
        model.addAttribute("rows", data.get("rows"));
        
        return "era-units"; // Chemin vers le fichier HTML
    }
}