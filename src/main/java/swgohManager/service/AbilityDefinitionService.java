package swgohManager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swgohManager.client.SwgohDataClient;
import swgohManager.client.dto.AbilityRaw;
import swgohManager.model.AbilityDefinition;
import swgohManager.repository.AbilityDefinitionRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbilityDefinitionService {

    private final SwgohDataClient swgohDataClient;
    private final AbilityDefinitionRepository abilityDefinitionRepository;
    private final LocalizationService localizationService; // Injecté ici

    @Transactional
    public String synchroniserAbilities() {
        String version = swgohDataClient.getLatestGameVersion();
        List<AbilityRaw> abilitiesRaw = swgohDataClient.getAbilities(version);

        abilityDefinitionRepository.deleteAllInBatch();

        List<AbilityDefinition> aSauvegarder = new ArrayList<>();

        for (AbilityRaw raw : abilitiesRaw) {
            
            // Résolution des traductions en français
            String nameTraduit = localizationService.traduire(raw.nameKey());
            String descTraduite = localizationService.traduire(raw.descKey());

            aSauvegarder.add(AbilityDefinition.builder()
                    .id(raw.id())
                    .nameKey(raw.nameKey())
                    .name(nameTraduit)
                    .descKey(raw.descKey())
                    .description(descTraduite)
                    .shortDescKey(raw.shortDescKey())
                    .icon(raw.icon())
                    .cooldown(raw.cooldown())
                    .abilityType(raw.abilityType())
                    .buttonLocation(raw.buttonLocation())
                    .detailLocation(raw.detailLocation())
                    .cooldownType(raw.cooldownType())
                    .useAsReinforcementDesc(raw.useAsReinforcementDesc())
                    .blockingEffectId(raw.blockingEffectId())
                    .blockedLocKey(raw.blockedLocKey())
                    .grantedPriority(raw.grantedPriority())
                    .subIcon(raw.subIcon())
                    .allyTargetingRuleId(raw.allyTargetingRuleId())
                    .build());
        }

        abilityDefinitionRepository.saveAll(aSauvegarder);

        String resultat = String.format("%d capacités enregistrées et traduites", aSauvegarder.size());
        log.info(resultat);
        return resultat;
    }
}