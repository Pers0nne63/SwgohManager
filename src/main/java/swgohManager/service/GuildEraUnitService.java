package swgohManager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swgohManager.controller.dto.PlayerEraRowDTO;
import swgohManager.repository.EraUnitPlayerProjection;
import swgohManager.repository.PlayerEraUnitStatusActuelRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuildEraUnitService {

    private final PlayerEraUnitStatusActuelRepository repository;

    // Structure pour les en-têtes de colonnes
    public record EraUnitHeaderDTO(String baseId, String libelle) {}

    public Map<String, Object> getGuildeEraData() {
        List<EraUnitPlayerProjection> rawData = repository.findEraUnitsWithRosterData();

        // 1. Déterminer le libellé de chaque unitBaseId
        Map<String, String> unitLabelsMap = new LinkedHashMap<>();
        for (EraUnitPlayerProjection row : rawData) {
            String baseId = row.getUnitBaseId();
            if (baseId != null) {
                String libelle = row.getLibelle();
                // Si la clé n'existe pas ou si on avait mis le baseId par défaut et qu'on trouve enfin un vrai libellé
                if (!unitLabelsMap.containsKey(baseId) || 
                   (unitLabelsMap.get(baseId).equals(baseId) && libelle != null && !libelle.isBlank())) {
                    unitLabelsMap.put(baseId, (libelle != null && !libelle.isBlank()) ? libelle : baseId);
                }
            }
        }

        // 2. Transformer la Map en liste d'objets d'en-tête et trier par libellé
        List<EraUnitHeaderDTO> columns = unitLabelsMap.entrySet().stream()
                .map(entry -> new EraUnitHeaderDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(EraUnitHeaderDTO::libelle))
                .collect(Collectors.toList());

        // 3. Grouper les données par joueur
        Map<String, PlayerEraRowDTO> rowsByPlayer = new LinkedHashMap<>();
        
        for (EraUnitPlayerProjection row : rawData) {
            String playerName = row.getPlayerName();
            if (playerName == null) continue;

            rowsByPlayer.putIfAbsent(playerName, new PlayerEraRowDTO());
            PlayerEraRowDTO dto = rowsByPlayer.get(playerName);
            dto.setPlayerId(row.getPlayerId());
            dto.setPlayerName(playerName);

            String etoiles = (row.getRarity() != null) ? row.getRarity() + "★" : "0★";
            String eraLevel = (row.getEraLevel() != null) ? "N" + row.getEraLevel() : "N0";
            
            dto.getUnitData().put(row.getUnitBaseId(), etoiles + " - " + eraLevel);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("columns", columns);
        result.put("rows", new ArrayList<>(rowsByPlayer.values()));
        return result;
    }
}