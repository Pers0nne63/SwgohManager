package swgohManager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import swgohManager.controller.dto.DatacronAffixOptionProjection;
import swgohManager.controller.dto.DatacronDefinitionProjection;
import swgohManager.model.DatacronAffixTemplate;

public interface DatacronAffixTemplateRepository extends JpaRepository<DatacronAffixTemplate, Long> {
    void deleteAllInBatch();

    @Query(value = """
        select dat.ability_id as "abilityId",
                dat.target_rule as "target",
                cd.libelle as "libelle",
                dat.tier as "tier",
                dt.focused as "focused",
                dt.set_id as "set",
                dat.stat_type as "statType",
                sd."libellé" as "statLibelle",
                replace(ad.description, '{0}', cd.libelle) as "description"
        from datacron_affix_template dat
        left outer join datacron_template dt on dt.id_template = dat.id_template
        left outer join battle_targeting_rule_category btrc on btrc.battle_targeting_rule_id = dat.target_rule
        left outer join category_definition cd on cd.id = btrc.category_id
        left outer join ability_definition ad on dat.ability_id = ad.id
        left outer join stat_definition sd on sd.stat_id = cast(dat.stat_type as numeric)
        where dt.set_id in (select distinct set_id from datacron_template dt2 order by dt2.set_id desc limit 4)
        """, nativeQuery = true)
    List<DatacronAffixOptionProjection> findOptionsFarmPlan();
    
    @Query(value = """
    	    select dat.id_template as "idTemplate",
    	           dt.set_id as "set",
    	           dt.focused as "focused",
    	           case when dat.id_template like '%focused%' then split_part(dat.id_template, '_', 5) else null end as "focusLibelle",
    	           dat.tier as "tier",
    	           dat.ability_id as "abilityId",
    	           split_part(dat.target_rule, '_', 3) as "target",
    	           cd.libelle as "libelle",
    	           replace(ad.description, '{0}', cd.libelle) as "description",
    	           dat.stat_type as "statType",
    	           sd."libellé" as "statLibelle",
    	           round(cast(dat.stat_value_min as numeric) / 1000000, 2) as "statMin",
    	           round(cast(dat.stat_value_max as numeric) / 1000000, 2) as "statMax"
    	    from datacron_affix_template dat
    	    left outer join datacron_template dt on dt.id_template = dat.id_template
    	    left outer join battle_targeting_rule_category btrc on btrc.battle_targeting_rule_id = dat.target_rule
    	    left outer join category_definition cd on cd.id = btrc.category_id
    	    left outer join ability_definition ad on dat.ability_id = ad.id
    	    left outer join stat_definition sd on sd.stat_id = cast(dat.stat_type as numeric)
    	    where dt.set_id in (select distinct set_id from datacron_template dt2 order by dt2.set_id desc limit 4)
    	    """, nativeQuery = true)
    	List<DatacronDefinitionProjection> findDefinitions();
}