package swgohManager.service;


import swgohManager.client.dto.GuildResponse;
import swgohManager.model.Joueur;
import swgohManager.repository.JoueurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JoueurService {

    private final JoueurRepository joueurRepository;

    @Value("${swgoh.guild.id}")
    private String guildId;

    @Transactional
    public String synchroniserJoueurs(GuildResponse response) {
        
        if (response == null || response.guild() == null) {
            log.warn("Réponse de guilde invalide");
            return "Aucune donnée";
        }
        List<GuildResponse.Member> membres = response.guild().member();

        if (membres == null || membres.isEmpty()) {
            log.warn("Aucun membre retourné par l'API pour la guilde {}", guildId);
            return "Aucun membre trouvé";
        }

        Set<String> playerIdsPresents = membres.stream()
                .map(GuildResponse.Member::playerId)
                .collect(Collectors.toSet());

        int nouveaux = 0;
        int misAJour = 0;

        for (GuildResponse.Member m : membres) {
            Joueur joueur = joueurRepository.findByPlayerId(m.playerId()).orElse(null);
            boolean estNouveau = (joueur == null);

            if (estNouveau) {
                joueur = new Joueur();
                joueur.setPlayerId(m.playerId());
            }

            joueur.setPlayerName(m.playerName());
            joueur.setGuildId(guildId);
            joueur.setGalacticPower(parseLong(m.galacticPower()));
            joueur.setLeagueId(m.leagueId());
            joueur.setShipGalacticPower(parseLong(m.shipGalacticPower()));
            joueur.setCharacterGalacticPower(parseLong(m.characterGalacticPower()));
            joueur.setPresentInGuild(true);

            joueurRepository.save(joueur);

            if (estNouveau) nouveaux++; else misAJour++;
        }

        // Marquer comme absents les joueurs qui étaient présents mais ne le sont plus
        List<Joueur> devenusAbsents = joueurRepository.findAllByPresentInGuildTrue().stream()
                .filter(j -> !playerIdsPresents.contains(j.getPlayerId()))
                .toList();

        devenusAbsents.forEach(j -> j.setPresentInGuild(false));
        joueurRepository.saveAll(devenusAbsents);

        String resultat = String.format(
                "%d nouveau(x) joueur(s), %d mis à jour, %d marqué(s) absent(s)",
                nouveaux, misAJour, devenusAbsents.size()
        );
        log.info(resultat);
        return resultat;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Impossible de parser la valeur numérique : {}", value);
            return null;
        }
    }
}