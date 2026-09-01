package swgohManager.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.SwgohDataClient;
import swgohManager.client.dto.BattleTargetingRuleRaw;
import swgohManager.model.BattleTargetingRuleCategory;
import swgohManager.repository.BattleTargetingRuleCategoryRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class BattleTargetingRuleService {

    private final SwgohDataClient swgohDataClient;
    private final BattleTargetingRuleCategoryRepository battleTargetingRuleCategoryRepository;

    @Transactional
    public String synchroniserBattleTargetingRules() {
        String version = swgohDataClient.getLatestGameVersion();
        List<BattleTargetingRuleRaw> rulesRaw = swgohDataClient.getBattleTargetingRules(version);

        battleTargetingRuleCategoryRepository.deleteAllInBatch();

        List<BattleTargetingRuleCategory> aSauvegarder = new ArrayList<>();
        for (BattleTargetingRuleRaw raw : rulesRaw) {
            if (raw.category() == null || raw.category().category() == null) {
                continue;
            }
            for (BattleTargetingRuleRaw.CategoryEntryRaw entry : raw.category().category()) {
                if (Boolean.FALSE.equals(entry.exclude())) {
                    aSauvegarder.add(BattleTargetingRuleCategory.builder()
                            .battleTargetingRuleId(raw.id())
                            .categoryId(entry.categoryId())
                            .build());
                }
            }
        }

        battleTargetingRuleCategoryRepository.saveAll(aSauvegarder);

        String resultat = String.format("%d catégorie(s) de ciblage enregistrée(s) (issues de %d règle(s))",
                aSauvegarder.size(), rulesRaw.size());
        log.info(resultat);
        return resultat;
    }
}