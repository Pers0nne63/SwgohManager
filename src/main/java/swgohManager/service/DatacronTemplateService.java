package swgohManager.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.SwgohDataClient;
import swgohManager.client.dto.DatacronAffixTemplateSetRaw;
import swgohManager.client.dto.DatacronTemplateRaw;
import swgohManager.model.DatacronAffixTemplate;
import swgohManager.model.DatacronTemplate;
import swgohManager.model.DatacronTierAffixSetTemplate;
import swgohManager.model.DatacronTierTemplate;
import swgohManager.repository.DatacronAffixTemplateRepository;
import swgohManager.repository.DatacronTemplateRepository;
import swgohManager.repository.DatacronTierAffixSetTemplateRepository;
import swgohManager.repository.DatacronTierTemplateRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatacronTemplateService {

    private final SwgohDataClient swgohDataClient;
    private final DatacronTemplateRepository datacronTemplateRepository;
    private final DatacronTierTemplateRepository datacronTierTemplateRepository;
    private final DatacronTierAffixSetTemplateRepository datacronTierAffixSetTemplateRepository;
    private final DatacronAffixTemplateRepository datacronAffixTemplateRepository;

    @Transactional
    public String synchroniserDatacrons() {
        String version = swgohDataClient.getLatestGameVersion();

        List<DatacronTemplateRaw> templatesRaw = swgohDataClient.getDatacronTemplates(version);
        List<DatacronAffixTemplateSetRaw> affixesRaw = swgohDataClient.getDatacronAffixes(version);

        // Nettoyage en cascade
        datacronAffixTemplateRepository.deleteAllInBatch();
        datacronTierAffixSetTemplateRepository.deleteAllInBatch();
        datacronTierTemplateRepository.deleteAllInBatch();
        datacronTemplateRepository.deleteAllInBatch();

        Map<String, List<DatacronAffixTemplateSetRaw.AffixRaw>> affixDictionnaire = new HashMap<>();
        for (DatacronAffixTemplateSetRaw set : affixesRaw) {
            affixDictionnaire.put(set.id(), set.affix());
        }

        List<DatacronTemplate> templatesASauvegarder = new ArrayList<>();
        List<DatacronTierTemplate> tiersASauvegarder = new ArrayList<>();
        List<DatacronTierAffixSetTemplate> tierAffixSetsASauvegarder = new ArrayList<>();
        List<DatacronAffixTemplate> affixesASauvegarder = new ArrayList<>();

        for (DatacronTemplateRaw raw : templatesRaw) {
            
            // 1. Template Parent complet
            templatesASauvegarder.add(DatacronTemplate.builder()
                    .idTemplate(raw.id())
                    .setId(raw.setId())
                    .initialTiers(raw.initialTiers())
                    .referenceTemplateId(raw.referenceTemplateId())
                    .maxRerolls(raw.maxRerolls())
                    .allowReroll(raw.allowReroll())
                    .focused(raw.focused())
                    .focusedIcon(raw.focusedIcon())
                    .focusedPrefab(raw.focusedPrefab())
                    .build());

            if (raw.tier() != null) {
                for (DatacronTemplateRaw.DatacronTierRaw tier : raw.tier()) {
                    
                    // 2. Tier complet
                    tiersASauvegarder.add(DatacronTierTemplate.builder()
                            .idTemplate(raw.id())
                            .tierId(tier.id())
                            .requiredUnitTier(tier.requiredUnitTier())
                            .requiredRelicTier(tier.requiredRelicTier())
                            .overrideUpgradeCostRecipeId(tier.overrideUpgradeCostRecipeId())
                            .overrideScopeIdentifier(tier.overrideScopeIdentifier())
                            .build());
                    
                    // 3. Identifiants des jeux d'affixes bruts (TierAffixSet)
                    if (tier.affixTemplateSetId() != null) {
                        for (String affixSetId : tier.affixTemplateSetId()) {
                            tierAffixSetsASauvegarder.add(DatacronTierAffixSetTemplate.builder()
                                    .idTemplate(raw.id())
                                    .tierId(tier.id())
                                    .affixTemplateSetId(affixSetId)
                                    .build());

                            // 4. Résolution détaillée dans DatacronAffixTemplate
                            List<DatacronAffixTemplateSetRaw.AffixRaw> detailAffixes = affixDictionnaire.get(affixSetId);
                            if (detailAffixes != null) {
                                for (DatacronAffixTemplateSetRaw.AffixRaw detail : detailAffixes) {
                                    String cleanAbilityId = (detail.abilityId() != null && !detail.abilityId().isEmpty()) 
                                            ? detail.abilityId() : null;

                                    affixesASauvegarder.add(DatacronAffixTemplate.builder()
                                            .idTemplate(raw.id())
                                            .affixSetId(affixSetId)
                                            .tier(tier.id())
                                            .statType(detail.statType() != null ? String.valueOf(detail.statType()) : null)
                                            .statValueMin(detail.statValueMin())
                                            .statValueMax(detail.statValueMax())
                                            .scopeIcon(detail.scopeIcon())
                                            .targetRule(detail.targetRule())
                                            .abilityId(cleanAbilityId)
                                            .build());
                                }
                            }
                        }
                    }
                }
            }
        }

        datacronTemplateRepository.saveAll(templatesASauvegarder);
        datacronTierTemplateRepository.saveAll(tiersASauvegarder);
        datacronTierAffixSetTemplateRepository.saveAll(tierAffixSetsASauvegarder);
        datacronAffixTemplateRepository.saveAll(affixesASauvegarder);

        String resultat = String.format("%d Templates, %d Tiers, %d AffixSets et %d Affixes enregistrés", 
                templatesASauvegarder.size(), tiersASauvegarder.size(), tierAffixSetsASauvegarder.size(), affixesASauvegarder.size());
        log.info(resultat);
        
        return resultat;
    }
}