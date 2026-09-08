package swgohManager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import swgohManager.model.TbImportActivite;
import swgohManager.model.TbImportScoreJoueur;
import swgohManager.dto.*;

public interface TbImportScoreJoueurRepository extends JpaRepository<TbImportScoreJoueur, Long> {
    Optional<TbImportScoreJoueur> findByTbImportActiviteAndPlayerId(TbImportActivite tbImportActivite, String playerId);
    
 // Récupère tous les scores d'une TB par joueur et par round
    @Query(value = """
    	    SELECT j.player_name AS playerName, 
			       tpr2.planete_name AS planeteName, 
			       CASE 
			           WHEN tpr2.planete_id = tpr.ls_planete_id THEN 'LS'
			           WHEN tpr2.planete_id = tpr.ds_planete_id THEN 'DS'
			           WHEN tpr2.planete_id = tpr.mix_planete_id THEN 'Mix'
			           WHEN tpr2.planete_id = tpr.zeffo_planete_id THEN 'Zeffo'
			           WHEN tpr2.planete_id = tpr.mandalore_planete_id THEN 'Mandalore'
			           ELSE 'Autre'
			       END AS zone,
			       tpr.round_num AS roundNum, 
			       tia.stat_type AS statType, 
			       tisj.score AS score
			FROM tb_plan_round tpr   
			LEFT OUTER JOIN tb_plan_template tpt ON tpt.id = tpr.plan_id 
			LEFT OUTER JOIN territory_battle tb ON tb.plan_id = tpt.id  
			LEFT OUTER JOIN tb_import_activite tia ON tia.round_num = tpr.round_num AND tia.territory_battle_id = tb.id 
			LEFT OUTER JOIN tb_import_score_joueur tisj ON tisj.tb_import_activite_id = tia.id 
			LEFT OUTER JOIN joueurs j ON j.player_id = tisj.player_id
			LEFT OUTER JOIN tb_planete_reference tpr2 ON tpr2.conflict = tia.conflict AND tpr2.phase = tia.phase AND tpr2.bonus = tia.bonus 
			WHERE tb.id = :tbId
			ORDER BY j.player_name, tpr2.planete_name, tia.map_stat_id
    	    """, nativeQuery = true)
    	List<TbScoreFlatProjection> findScoresByTbIdNative(@Param("tbId") Long tbId);
}