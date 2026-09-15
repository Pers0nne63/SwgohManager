package swgohManager.repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import swgohManager.model.TwHistorique;

public interface TwHistoriqueRepository extends JpaRepository<TwHistorique, Long> {

    Optional<TwHistorique> findByGuildIdAndOpponentGuildIdAndStartTimeAndEndTime(
            String guildId, String opponentGuildId, Instant startTime, Instant endTime);

    /** Historique complet, conservé indéfiniment — à utiliser pour l'archive/export, pas pour l'affichage courant. */
    List<TwHistorique> findByGuildIdOrderByEndTimeDesc(String guildId);

    /** Les 8 dernières uniquement — à utiliser pour le tableau détaillé de la page guilde. */
    List<TwHistorique> findTop8ByGuildIdOrderByEndTimeDesc(String guildId);

    @Query(value = """
        SELECT
            COUNT(*)                                                          AS "nombre",
            AVG(pg_inscrite_nous)                                             AS "pgInscriteMoyenne",
            AVG(notre_score)                                                  AS "scoreMoyen",
            AVG(score_adversaire)                                             AS "scoreAdversaireMoyen",
            SUM(CASE WHEN notre_score > score_adversaire THEN 1 ELSE 0 END)   AS "victoires",
            SUM(CASE WHEN notre_score < score_adversaire THEN 1 ELSE 0 END)   AS "defaites",
            AVG(notre_score - score_adversaire)                               AS "ecartMoyen"
        FROM (
            SELECT *
            FROM tw_historique
            WHERE guild_id = :guildId
            ORDER BY end_time DESC
            LIMIT 8
        ) derniere_tw
        """, nativeQuery = true)
    TwStatsProjection findStatsAgregeesByGuildId(String guildId);

    interface TwStatsProjection {
        Long getNombre();
        Double getPgInscriteMoyenne();
        Double getScoreMoyen();
        Double getScoreAdversaireMoyen();
        Long getVictoires();
        Long getDefaites();
        Double getEcartMoyen();
    }
}