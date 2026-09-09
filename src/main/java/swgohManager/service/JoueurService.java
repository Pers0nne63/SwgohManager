package swgohManager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swgohManager.client.dto.PlayerResponse;
import swgohManager.model.Joueur;
import swgohManager.repository.JoueurRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class JoueurService {

    private final JoueurRepository joueurRepository;

    @Transactional
    public Joueur mettreAJourDepuisPlayer(PlayerResponse response) {
        if (response == null || response.playerId() == null) {
            log.warn("Tentative de mise à jour d'un joueur avec une réponse invalide.");
            return null;
        }

        // Le joueur a normalement déjà été créé par la synchro Guilde (qui gère les allées et venues), 
        // mais on sécurise au cas où il serait appelé indépendamment.
        Joueur joueur = joueurRepository.findByPlayerId(response.playerId())
                .orElseGet(() -> Joueur.builder()
                        .playerId(response.playerId())
                        .build());

        // 1. Mise à jour des informations de base
        joueur.setPlayerName(response.name());
        
        if (response.guildId() != null) {
            joueur.setGuildId(response.guildId());
        }
        if (response.guildName() != null) {
            joueur.setGuildName(response.guildName());
        }

        // 2. Récupération des Galactic Power via les méthodes utilitaires du DTO
        joueur.setGalacticPower(response.getGalacticPower());
        joueur.setCharacterGalacticPower(response.getCharacterGalacticPower());
        joueur.setShipGalacticPower(response.getShipGalacticPower());

        // 3. Récupération de la ligue GAC
        if (response.playerRating() != null && response.playerRating().playerRankStatus() != null) {
            joueur.setLeagueId(response.playerRating().playerRankStatus().leagueId());
        }

        // Sécurité : si on interroge son endpoint, c'est qu'il est actif (le GuildeService s'occupera de le marquer absent le cas échéant)
        joueur.setPresentInGuild(true);

        // 4. Sauvegarde en base
        return joueurRepository.save(joueur);
    }
}