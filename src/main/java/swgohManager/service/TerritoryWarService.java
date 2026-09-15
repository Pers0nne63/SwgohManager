package swgohManager.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import swgohManager.client.SwgohApiClient;
import swgohManager.client.dto.GuildResponse;
import swgohManager.model.TwHistorique;
import swgohManager.repository.TwHistoriqueRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerritoryWarService {

    private static final int NB_TW_A_CONSERVER = 8;
    private static final long TOLERANCE_SECONDES = 300;

    private final TwHistoriqueRepository twHistoriqueRepository;
    private final SwgohApiClient swgohApiClient;

    @Value("${swgoh.guild.id}")
    private String guildId;

    @Transactional
    public String synchroniserTerritoryWar(GuildResponse response) {
        List<GuildResponse.TerritoryWarResult> twResults = response.guild().recentTerritoryWarResult();

        if (twResults == null || twResults.isEmpty()) {
            log.warn("Aucune Territory War disponible pour la guilde {}", guildId);
            return "Aucune Territory War trouvée";
        }

        int nouveaux = 0;
        int misAJour = 0;
        int scansEvites = 0;
        int scansEffectues = 0;

        for (GuildResponse.TerritoryWarResult tw : twResults) {
            if (tw.opponentGuildProfile() == null) {
                log.warn("TW sans opponentGuildProfile, ignorée");
                continue;
            }

            Instant startTime = parseEpochSeconds(tw.startTime());
            Instant endTime = parseEpochSeconds(tw.endTimeSeconds());
            String opponentGuildId = tw.opponentGuildProfile().id();

            Optional<TwHistorique> existant = twHistoriqueRepository
                    .findByGuildIdAndOpponentGuildIdAndStartTimeAndEndTime(guildId, opponentGuildId, startTime, endTime);

            TwHistorique entite = existant.orElseGet(TwHistorique::new);
            boolean estNouveau = entite.getId() == null;

            entite.setGuildId(guildId);
            entite.setTerritoryWarId(tw.territoryWarId());
            entite.setStartTime(startTime);
            entite.setEndTime(endTime);
            entite.setNotreScore(parseLongOrNull(tw.score()));
            entite.setScoreAdversaire(parseLongOrNull(tw.opponentScore()));
            entite.setPgInscriteAdversaire(tw.power() != null ? tw.power().longValue() : null);
            entite.setOpponentGuildId(opponentGuildId);
            entite.setOpponentGuildName(tw.opponentGuildProfile().name());
            entite.setOpponentGuildGP(parseLongOrNull(tw.opponentGuildProfile().guildGalacticPower()));

            // Le point clé : id opposant + datetime déjà en BDD ET déjà résolu -> pas besoin de rescanner
            if (existant.isPresent() && entite.getPgInscriteNous() != null) {
                scansEvites++;
            } else {
                Long pgResolue = resoudrePgInscriteGenerique(
                        entite.getOpponentGuildId(), guildId, entite.getStartTime(), entite.getEndTime());
                if (pgResolue != null) {
                    entite.setPgInscriteNous(pgResolue);
                }
                scansEffectues++;
            }

            twHistoriqueRepository.save(entite);
            if (estNouveau) nouveaux++; else misAJour++;
        }

        String resultat = String.format(
                "TW : %d nouvelle(s), %d mise(s) à jour, %d scan(s) adversaire évité(s), %d scan(s) effectué(s)",
                nouveaux, misAJour, scansEvites, scansEffectues);
        log.info(resultat);
        return resultat;
    }

    
    

    public Long resoudrePgInscrite(GuildResponse.TerritoryWarResult twDeReference, String guildIdCible) {
        if (twDeReference.opponentGuildProfile() == null) return null;

        Instant startRef = parseEpochSeconds(twDeReference.startTime());
        Instant endRef = parseEpochSeconds(twDeReference.endTimeSeconds());

        return resoudrePgInscriteGenerique(
                twDeReference.opponentGuildProfile().id(), guildIdCible, startRef, endRef);
    }
    
    

    private Long resoudrePgInscriteGenerique(String opponentGuildId, String guildIdCible,
            Instant startRef, Instant endRef) {
			if (opponentGuildId == null) return null;
			
			GuildResponse adversaire;
			try {
			adversaire = swgohApiClient.getGuild(opponentGuildId);
			} catch (Exception e) {
			log.warn("Scan impossible de la guilde adverse {} pour résoudre la PG inscrite de {}",
			opponentGuildId, guildIdCible, e);
			return null;
			}
			
			List<GuildResponse.TerritoryWarResult> twAdversaire =
			adversaire.guild() != null ? adversaire.guild().recentTerritoryWarResult() : null;
			if (twAdversaire == null) return null;
			
			Optional<GuildResponse.TerritoryWarResult> miroir =
			trouverTwMiroir(twAdversaire, guildIdCible, startRef, endRef);
			
			if (miroir.isEmpty()) {
			log.warn("Aucune TW miroir fiable trouvée chez {} pour la guilde {}", opponentGuildId, guildIdCible);
			return null;
			}
			return miroir.get().power() != null ? miroir.get().power().longValue() : null;
			}
			
    private Optional<GuildResponse.TerritoryWarResult> trouverTwMiroir(
            List<GuildResponse.TerritoryWarResult> twAdversaire, String guildIdCible,
            Instant startRef, Instant endRef) {

        List<GuildResponse.TerritoryWarResult> candidats = twAdversaire.stream()
                .filter(tw -> tw.opponentGuildProfile() != null && guildIdCible.equals(tw.opponentGuildProfile().id()))
                .toList();

        if (candidats.isEmpty()) return Optional.empty();

        Optional<GuildResponse.TerritoryWarResult> meilleur = candidats.stream()
                .min(Comparator.comparingLong(tw -> ecartTotal(tw, startRef, endRef)));

        return meilleur.filter(tw -> ecartTotal(tw, startRef, endRef) <= TOLERANCE_SECONDES * 2);
    }

		    private long ecartTotal(GuildResponse.TerritoryWarResult tw, Instant startRef, Instant endRef) {
		        Instant debut = parseEpochSeconds(tw.startTime());
		        Instant fin = parseEpochSeconds(tw.endTimeSeconds());
		        if (debut == null || fin == null || startRef == null || endRef == null) return Long.MAX_VALUE;

		        long ecartStart = Math.abs(debut.getEpochSecond() - startRef.getEpochSecond());
		        long ecartEnd = Math.abs(fin.getEpochSecond() - endRef.getEpochSecond());
		        return ecartStart + ecartEnd;
		    }


    private Instant parseEpochSeconds(String value) {
        return (value == null || value.isBlank()) ? null : Instant.ofEpochSecond(Long.parseLong(value));
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}