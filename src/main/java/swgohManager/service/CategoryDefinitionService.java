package swgohManager.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.SwgohDataClient;
import swgohManager.client.dto.CategoryRaw;
import swgohManager.model.CategoryDefinition;
import swgohManager.repository.CategoryDefinitionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryDefinitionService {

    private final SwgohDataClient swgohDataClient;
    private final CategoryDefinitionRepository categoryDefinitionRepository;
    private final LocalizationService localizationService;

    @Transactional
    public String synchroniserCategories() {
        String version = swgohDataClient.getLatestGameVersion();
        List<CategoryRaw> categoriesRaw = swgohDataClient.getCategories(version);

        categoryDefinitionRepository.deleteAllInBatch();

        List<CategoryDefinition> aSauvegarder = categoriesRaw.stream()
                .map(raw -> CategoryDefinition.builder()
                        .id(raw.id())
                        .descKey(raw.descKey())
                        .libelle(localizationService.traduire(raw.descKey()))
                        .build())
                .toList();

        categoryDefinitionRepository.saveAll(aSauvegarder);

        String resultat = String.format("%d Category(ies) enregistrée(s)", aSauvegarder.size());
        log.info(resultat);
        return resultat;
    }
}