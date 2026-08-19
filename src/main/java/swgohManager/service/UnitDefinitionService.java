package swgohManager.service;

import swgohManager.client.SwgohDataClient;
import swgohManager.client.dto.RelicTierDefinitionRaw;
import swgohManager.client.dto.UnitRaw;
import swgohManager.client.dto.UnitSegmentData;
import swgohManager.model.*;
import swgohManager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnitDefinitionService {

    private final SwgohDataClient swgohDataClient;
    private final UnitDefinitionRepository unitDefinitionRepository;
    private final UnitTierDefinitionRepository unitTierDefinitionRepository;
    private final UnitBaseStatDefinitionRepository unitBaseStatDefinitionRepository;
    private final UnitRelicDefinitionRepository unitRelicDefinitionRepository;
    private final RelicTierDefinitionRepository relicTierDefinitionRepository;

    @Transactional
    public String synchroniserUnites() {
        String version = swgohDataClient.getLatestGameVersion();
        UnitSegmentData segment = swgohDataClient.streamUnitSegment(version);

        String resultatUnites = traiterUnites(segment.units(), version);
        String resultatRelicTier = traiterRelicTierDefinitions(segment.relicTierDefinitions());

        String resultat = resultatUnites + " | " + resultatRelicTier;
        log.info(resultat);
        return resultat;
    }

    private String traiterUnites(List<UnitRaw> unitesBrutes, String version) {
        Map<String, UnitDefinition> unitesExistantes = unitDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(UnitDefinition::getIdUnit, u -> u));
        Map<String, UnitTierDefinition> tiersExistants = unitTierDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(t -> t.getIdUnit() + "|" + t.getGear() + "|" + t.getStat(), t -> t));
        Map<String, UnitBaseStatDefinition> baseStatsExistants = unitBaseStatDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(b -> b.getIdUnit() + "|" + b.getStat(), b -> b));
        Map<String, UnitRelicDefinition> relicsExistants = unitRelicDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(r -> r.getIdUnit() + "|" + r.getRelicTierDefinitionId(), r -> r));

        List<UnitTierDefinition> tiersASauver = new ArrayList<>();
        List<UnitBaseStatDefinition> baseStatsASauver = new ArrayList<>();
        List<UnitRelicDefinition> relicsASauver = new ArrayList<>();
        List<UnitDefinition> unitesASauver = new ArrayList<>();

        for (UnitRaw u : unitesBrutes) {
            UnitDefinition unite = unitesExistantes.get(u.id());
            if (unite == null) {
                unite = new UnitDefinition();
                unite.setIdUnit(u.id());
                unitesExistantes.put(u.id(), unite);
            }
            unite.setBaseId(u.baseId());
            unite.setForceAlignment(u.forceAlignment());
            unite.setUnitClass(u.unitClass());
            unite.setCombatType(u.combatType());
            unite.setLegend(u.legend());
            unite.setStatProgressionId(u.statProgressionId());

            String role = extraireRole(u.categoryId());
            String primaryStat = convertirPrimaryStat(u.primaryUnitStat());
            String masteryClass = (role != null && primaryStat != null) ? primaryStat + "_" + role : null;
            unite.setRole(role);
            unite.setPrimaryStat(primaryStat);
            unite.setMasteryClass(masteryClass);

            unite.setGameVersion(version);
            unitesASauver.add(unite);

            if (u.unitTier() != null) {
                for (UnitRaw.UnitTierRaw ut : u.unitTier()) {
                    if (ut.baseStat() == null || ut.baseStat().stat() == null) continue;
                    for (UnitRaw.StatEntry entry : ut.baseStat().stat()) {
                        String cle = u.id() + "|" + ut.tier() + "|" + entry.unitStatId();
                        UnitTierDefinition t = tiersExistants.get(cle);
                        if (t == null) {
                            t = new UnitTierDefinition();
                            t.setIdUnit(u.id());
                            t.setGear(ut.tier());
                            t.setStat(entry.unitStatId());
                            tiersExistants.put(cle, t);
                        }
                        t.setValeur(parseLong(entry.unscaledDecimalValue()));
                        tiersASauver.add(t);
                    }
                }
            }

            if (u.baseStat() != null && u.baseStat().stat() != null) {
                for (UnitRaw.StatEntry entry : u.baseStat().stat()) {
                    String cle = u.id() + "|" + entry.unitStatId();
                    UnitBaseStatDefinition b = baseStatsExistants.get(cle);
                    if (b == null) {
                        b = new UnitBaseStatDefinition();
                        b.setIdUnit(u.id());
                        b.setStat(entry.unitStatId());
                        baseStatsExistants.put(cle, b);
                    }
                    b.setValeur(parseLong(entry.unscaledDecimalValue()));
                    baseStatsASauver.add(b);
                }
            }

            if (u.relicDefinition() != null && u.relicDefinition().relicTierDefinitionId() != null) {
                for (String relicId : u.relicDefinition().relicTierDefinitionId()) {
                    String cle = u.id() + "|" + relicId;
                    UnitRelicDefinition r = relicsExistants.get(cle);
                    if (r == null) {
                        r = UnitRelicDefinition.builder()
                                .idUnit(u.id())
                                .relicTierDefinitionId(relicId)
                                .build();
                        relicsExistants.put(cle, r);
                    }
                    relicsASauver.add(r);
                }
            }
        }

        unitDefinitionRepository.saveAll(unitesASauver);
        unitTierDefinitionRepository.saveAll(tiersASauver);
        unitBaseStatDefinitionRepository.saveAll(baseStatsASauver);
        unitRelicDefinitionRepository.saveAll(relicsASauver);

        return String.format("%d unité(s), %d ligne(s) de tier, %d ligne(s) de base stat, %d ligne(s) de relic (référence)",
                unitesASauver.size(), tiersASauver.size(), baseStatsASauver.size(), relicsASauver.size());
    }

    private String traiterRelicTierDefinitions(List<RelicTierDefinitionRaw> relicsBrutes) {
        Map<String, RelicTierDefinition> existantes = relicTierDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(r -> r.getIdRelicTier() + "|" + r.getStat(), r -> r));

        List<RelicTierDefinition> aSauver = new ArrayList<>();

        for (RelicTierDefinitionRaw raw : relicsBrutes) {
            Integer relic = (raw.tier() != null && raw.tier() - 2 >= 0) ? raw.tier() - 2 : null;

            if (raw.stat() == null || raw.stat().stat() == null || raw.stat().stat().isEmpty()) {
                String cle = raw.id() + "|null";
                RelicTierDefinition r = existantes.get(cle);
                if (r == null) {
                    r = new RelicTierDefinition();
                    r.setIdRelicTier(raw.id());
                    existantes.put(cle, r);
                }
                r.setRelicStatTable(raw.relicStatTable());
                r.setTierRelic(raw.tier());
                r.setRelic(relic);
                aSauver.add(r);
                continue;
            }

            for (RelicTierDefinitionRaw.StatEntry entry : raw.stat().stat()) {
                String cle = raw.id() + "|" + entry.unitStatId();
                RelicTierDefinition r = existantes.get(cle);
                if (r == null) {
                    r = new RelicTierDefinition();
                    r.setIdRelicTier(raw.id());
                    r.setStat(entry.unitStatId());
                    existantes.put(cle, r);
                }
                r.setRelicStatTable(raw.relicStatTable());
                r.setTierRelic(raw.tier());
                r.setRelic(relic);
                r.setValeur(parseLong(entry.unscaledDecimalValue()));
                aSauver.add(r);
            }
        }

        relicTierDefinitionRepository.saveAll(aSauver);
        return String.format("%d ligne(s) de relicTierDefinition", aSauver.size());
    }

    private String extraireRole(List<String> categoryId) {
        if (categoryId == null) return null;
        if (categoryId.contains("role_tank")) return "TNK";
        if (categoryId.contains("role_support")) return "SUP";
        if (categoryId.contains("role_attacker")) return "ATK";
        if (categoryId.contains("role_healer")) return "HLR";
        return null;
    }

    private String convertirPrimaryStat(Integer primaryUnitStat) {
        if (primaryUnitStat == null) return null;
        return switch (primaryUnitStat) {
            case 2 -> "STR";
            case 3 -> "AGI";
            case 4 -> "TAC";
            default -> null;
        };
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Impossible de parser la valeur numérique : {}", value);
            return null;
        }
    }
}