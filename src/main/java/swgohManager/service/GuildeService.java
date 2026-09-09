package swgohManager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swgohManager.client.dto.GuildResponse;
import swgohManager.model.Guildes;
import swgohManager.model.Joueur;
import swgohManager.repository.GuildesRepository;
import swgohManager.repository.JoueurRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuildeService {

    private final GuildesRepository guildeRepository;
    private final JoueurRepository joueurRepository;

    @Value("${swgoh.guild.id}")
    private String guildId;

    @Transactional
    public String synchroniserGuilde(GuildResponse response) {
        if (response == null || response.guild() == null) {
            return "Aucune donnée de guilde";
        }

        List<GuildResponse.Member> membres = response.guild().member();
        if (membres == null || membres.isEmpty()) {
            return "Aucun membre trouvé";
        }

        // 1. Enregistrer ou mettre à jour la Guilde
        Guildes guilde = guildeRepository.findByGuildId(guildId)
                .orElseGet(() -> Guildes.builder().guildId(guildId).build());
        
        // (Optionnel) Si ton API te renvoie le nom de la guilde, tu peux le setter ici
        guilde.setName(response.guild().profile().name()); 
        guilde.setMemberCount(membres.size());
        guilde.setGP(response.guild().profile().guildGalacticPower());
        guildeRepository.save(guilde);

        // 2. Mettre à jour la présence des joueurs
        Set<String> playerIdsPresents = membres.stream()
                .map(GuildResponse.Member::playerId)
                .collect(Collectors.toSet());

        for (String pid : playerIdsPresents) {
            // On récupère le joueur, ou on crée une "coquille vide" s'il n'existe pas encore
            Joueur joueur = joueurRepository.findByPlayerId(pid)
                    .orElseGet(() -> Joueur.builder().playerId(pid).build());
            
            joueur.setGuildId(guildId);
            joueur.setPresentInGuild(true);
            
            // On ne met PLUS à jour les GP ici, ce sera fait par /player
            joueurRepository.save(joueur);
        }

        // 3. Marquer comme absents ceux qui ont quitté la guilde
        List<Joueur> devenusAbsents = joueurRepository.findAllByPresentInGuildTrue().stream()
                .filter(j -> !playerIdsPresents.contains(j.getPlayerId()))
                .toList();

        devenusAbsents.forEach(j -> j.setPresentInGuild(false));
        joueurRepository.saveAll(devenusAbsents);

        return String.format("Guilde synchronisée : %d membres actifs, %d partis", 
                playerIdsPresents.size(), devenusAbsents.size());
    }
}