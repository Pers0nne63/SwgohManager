package swgohManager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import swgohManager.model.Joueur;

public interface JoueurRepository extends JpaRepository<Joueur, Long> {

    Optional<Joueur> findByPlayerId(String playerId);
    

    List<Joueur> findAllByPresentInGuildTrue();
    
    List<Joueur> findByPresentInGuildTrueOrderByPlayerNameAsc();
    
    @Query("SELECT SUM(j.galacticPower) FROM Joueur j WHERE j.presentInGuild = true")
    Long sumGalacticPowerGuilde();
}