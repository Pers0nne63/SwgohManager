package swgohManager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swgohManager.model.StatDefinition;
import swgohManager.repository.StatDefinitionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatDefinitionService {

    private final StatDefinitionRepository statDefinitionRepository;
    private final LocalizationService localizationService;
    private record Entree(int statId, String nameKey, String descKey, boolean isDecimal, String name, String detailedName, String libellé, boolean isStatq) {}

    // Référentiel figé du jeu — ne devrait jamais évoluer.
    private static final List<Entree> DONNEES = List.of(
            new Entree(1, "UnitStat_Health", "UnitStatDescription_Health_TU7", false, "Health", "Max Health", "Santé",true),
            new Entree(2, "UnitStat_Strength", "UnitStatDescription_Strength", false, "Strength", "Strength", "Vigueur",false),
            new Entree(3, "UnitStat_Agility", "UnitStatDescription_Agility", false, "Agility", "Agility", "Agilité",false),
            new Entree(4, "UnitStat_Intelligence_TU7", "UnitStatDescription_Intelligence", false, "Tactics", "Tactics", "Tactiques",false),
            new Entree(5, "UnitStat_Speed", "UnitStatDescription_Speed", false, "Speed", "Speed", "Vitesse",true),
            new Entree(6, "UnitStat_AttackDamage", "UnitStatDescription_AttackDamage", false, "Physical Damage", "Physical Damage", "Dégats Physiques",true),
            new Entree(7, "UnitStat_AbilityPower", "UnitStatDescription_AbilityPower", false, "Special Damage", "Special Damage", "Dégats Spéciaux",true),
            new Entree(8, "UnitStat_Armor", "UnitStatDescription_Armor", false, "Armor", "Armor", "Armure",true),
            new Entree(9, "UnitStat_Suppression", "UnitStatDescription_Suppression", false, "Resistance", "Resistance", "Résistance",true),
            new Entree(10, "UnitStat_ArmorPenetration", "UnitStatDescription_ArmorPenetration", false, "Armor Penetration", "Armor Penetration", "Pénétration d'Armure",false),
            new Entree(11, "UnitStat_SuppressionPenetration", "UnitStatDescription_SuppressionPenetration", false, "Resistance Penetration", "Resistance Penetration", "Pénétration de Résistance",false),
            new Entree(12, "UnitStat_DodgeRating_TU5V", "UnitStatDescription_DodgeRating", false, "Dodge Chance", "Dodge Rating", "Chances d'esquives",false),
            new Entree(13, "UnitStat_DeflectionRating_TU5V", "UnitStatDescription_DeflectionRating", false, "Deflection Chance", "Deflection Rating", "Chances de déviation",false),
            new Entree(14, "UnitStat_AttackCriticalRating_TU5V", "UnitStatDescription_AttackCriticalRating", false, "Physical Critical Chance", "Physical Critical Rating", "Coups Critique Physique",true),
            new Entree(15, "UnitStat_AbilityCriticalRating_TU5V", "UnitStatDescription_AbilityCriticalRating", false, "Special Critical Chance", "Special Critical Rating", "Coups Critique Speciaux",true),
            new Entree(16, "UnitStat_CriticalDamage", "UnitStatDescription_CriticalDamage", true, "Critical Damage", "Critical Damage", "Dégats Critiques",false),
            new Entree(17, "UnitStat_Accuracy", "UnitStatDescription_Accuracy", true, "Potency", "Potency", "Pouvoir",true),
            new Entree(18, "UnitStat_Resistance", "UnitStatDescription_Resistance", true, "Tenacity", "Tenacity", "Ténacité",true),
            new Entree(19, "UnitStat_DodgePercentAdditive", "", true, "Dodge", "Dodge Percent Additive", "Dodge Percent Additive",false),
            new Entree(20, "UnitStat_DeflectionPercentAdditive", "", true, "Deflection", "Deflection Percent Additive", "Deflection Percent Additive",false),
            new Entree(21, "UnitStat_AttackCriticalPercentAdditive", "", true, "Physical Critical Chance", "Physical Critical Percent Additive", "Physical Critical Percent Additive",false),
            new Entree(22, "UnitStat_AbilityCriticalPercentAdditive", "", true, "Special Critical Chance", "Special Critical Percent Additive", "Special Critical Percent Additive",false),
            new Entree(23, "UnitStat_ArmorPercentAdditive", "", true, "Armor", "Armor Percent Additive", "Armor Percent Additive",false),
            new Entree(24, "UnitStat_SuppressionPercentAdditive", "", true, "Resistance", "Resistance Percent Additive", "Resistance Percent Additive",false),
            new Entree(25, "UnitStat_ArmorPenetrationPercentAdditive", "", true, "Armor Penetration", "Armor Penetration Percent Additive", "Armor Penetration Percent Additive",false),
            new Entree(26, "UnitStat_SuppressionPenetrationPercentAdditive", "", true, "Resistance Penetration", "Resistance Penetration Percent Additive", "Resistance Penetration Percent Additive",false),
            new Entree(27, "UnitStat_HealthSteal", "UnitStatDescription_HealthSteal", true, "Health Steal", "Health Steal", "Vol de Santé",false),
            new Entree(28, "UnitStat_MaxShield", "UnitStatDescription_MaxShield", false, "Protection", "Max Protection", "Protection",true),
            new Entree(29, "UnitStat_ShieldPenetration", "", true, "Protection Ignore", "Protection Ignore", "Protection Ignore",false),
            new Entree(30, "UnitStat_HealthRegen", "", true, "Health Regeneration", "Health Regen", "Health Regen",false),
            new Entree(31, "UnitStat_AttackDamagePercentAdditive", "", true, "Physical Damage", "Physical Damage Percent Additive", "Physical Damage Percent Additive",false),
            new Entree(32, "UnitStat_AbilityPowerPercentAdditive", "", true, "Special Damage", "Special Damage Percent Additive", "Special Damage Percent Additive",false),
            new Entree(33, "UnitStat_DodgeNegatePercentAdditive", "", true, "Physical Accuracy", "Dodge Negate Percent Additive", "Dodge Negate Percent Additive",false),
            new Entree(34, "UnitStat_DeflectionNegatePercentAdditive", "", true, "Special Accuracy", "Deflection Negate Percent Additive", "Deflection Negate Percent Additive",false),
            new Entree(35, "UnitStat_AttackCriticalNegatePercentAdditive", "", true, "Physical Critical Avoidance", "Physical Critical Negate Percent Additive", "Physical Critical Negate Percent Additive",false),
            new Entree(36, "UnitStat_AbilityCriticalNegatePercentAdditive", "", true, "Special Critical Avoidance", "Special Critical Negate Percent Additive", "Special Critical Negate Percent Additive",false),
            new Entree(37, "UnitStat_DodgeNegateRating", "UnitStatDescription_DodgeNegateRating", false, "Physical Accuracy", "Dodge Negate Rating", "Dodge Negate Rating",false),
            new Entree(38, "UnitStat_DeflectionNegateRating", "UnitStatDescription_DeflectionNegateRating", false, "Special Accuracy", "Deflection Negate Rating", "Deflection Negate Rating",false),
            new Entree(39, "UnitStat_AttackCriticalNegateRating", "UnitStatDescription_AttackCriticalNegateRating", false, "Physical Critical Avoidance", "Physical Critical Negate Rating", "Physical Critical Negate Rating",false),
            new Entree(40, "UnitStat_AbilityCriticalNegateRating", "UnitStatDescription_AbilityCriticalNegateRating", false, "Special Critical Avoidance", "Special Critical Negate Rating", "Special Critical Negate Rating",false),
            new Entree(41, "UnitStat_Offense", "UnitStatDescription_Offense", false, "Offense", "Offense", "Attaque",false),
            new Entree(42, "UnitStat_Defense", "UnitStatDescription_Defense", false, "Defense", "Defense", "Défense",false),
            new Entree(43, "UnitStat_DefensePenetration", "UnitStatDescription_DefensePenetration", false, "Defense Penetration", "Defense Penetration", "Pénétration de Défense",false),
            new Entree(44, "UnitStat_EvasionRating", "UnitStatDescription_EvasionRating", false, "Evasion", "Evasion Rating", "Evasion Rating",false),
            new Entree(45, "UnitStat_CriticalRating", "UnitStatDescription_CriticalRating", false, "Critical Chance", "Critical Rating", "Critical Rating",false),
            new Entree(46, "UnitStat_EvasionNegateRating", "UnitStatDescription_EvasionNegateRating", false, "Accuracy", "Evasion Negate Rating", "Evasion Negate Rating",false),
            new Entree(47, "UnitStat_CriticalNegateRating", "UnitStatDescription_CriticalNegateRating", false, "Critical Avoidance", "Critical Negate Rating", "Critical Negate Rating",false),
            new Entree(48, "UnitStat_OffensePercentAdditive", "", true, "Offense", "Offense Percent Additive", "Offense Percent Additive",false),
            new Entree(49, "UnitStat_DefensePercentAdditive", "", true, "Defense", "Defense Percent Additive", "Defense Percent Additive",false),
            new Entree(50, "UnitStat_DefensePenetrationPercentAdditive", "", true, "Defense Penetration", "Defense Penetration Percent Additive", "Defense Penetration Percent Additive",false),
            new Entree(51, "UnitStat_EvasionPercentAdditive", "", true, "Evasion", "Evasion Percent Additive", "Evasion Percent Additive",false),
            new Entree(52, "UnitStat_EvasionNegatePercentAdditive", "", true, "Accuracy", "Evasion Negate Percent Additive", "Evasion Negate Percent Additive",false),
            new Entree(53, "UnitStat_CriticalChancePercentAdditive", "", true, "Critical Chance", "Critical Chance Percent Additive", "Critical Chance Percent Additive",false),
            new Entree(54, "UnitStat_CriticalNegateChancePercentAdditive", "", true, "Critical Avoidance", "Critical Negate Chance Percent Additive", "Critical Negate Chance Percent Additive",false),
            new Entree(55, "UnitStat_MaxHealthPercentAdditive", "", true, "Health", "Max Health Percent Additive", "Max Health Percent Additive",false),
            new Entree(56, "UnitStat_MaxShieldPercentAdditive", "", true, "Protection", "Max Protection Percent Additive", "Max Protection Percent Additive",false),
            new Entree(57, "UnitStat_SpeedPercentAdditive", "", true, "Speed", "Speed Percent Additive", "Speed Percent Additive",false),
            new Entree(58, "UnitStat_CounterAttackRating", "", true, "Counter Attack", "Counter Attack Rating", "Counter Attack Rating",false),
            new Entree(59, "Combat_Buffs_TASK_NAME_2", "", true, "Taunt", "Taunt", "Taunt",false),
            new Entree(60, "UnitStat_DefensePenetrationTargetPercentAdditive", "UnitStatDescription_DefensePenetrationTargetPercentAdditive", true, "Defense Penetration", "Target Defense Penetration Percent Additive", "Target Defense Penetration Percent Additive",false),
            new Entree(61, "UNIT_STAT_STAT_VIEW_MASTERY", "", true, "Mastery", "Mastery", "Maîtrise",false)
    );

    @Transactional
    public String seedDonnees() {
        Map<Integer, StatDefinition> existantes = statDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(StatDefinition::getStatId, d -> d));

        List<StatDefinition> aSauvegarder = new ArrayList<>();

        for (Entree e : DONNEES) {
            StatDefinition d = existantes.get(e.statId());
            if (d == null) {
                d = new StatDefinition();
                d.setStatId(e.statId());
                existantes.put(e.statId(), d);
            }
            d.setNameKey(e.nameKey());
            d.setDescKey(e.descKey());
            d.setIsDecimal(e.isDecimal());
            d.setName(e.name());
            d.setDetailedName(e.detailedName());
            d.setLibellé(localizationService.traduire(e.nameKey()));            d.setIsStatq(e.isStatq());
            aSauvegarder.add(d);
        }

        statDefinitionRepository.saveAll(aSauvegarder);

        String resultat = String.format("%d ligne(s) de stat_definition chargée(s)", aSauvegarder.size());
        log.info(resultat);
        return resultat;
    }
}