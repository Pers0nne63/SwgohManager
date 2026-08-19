package swgohManager.repository;

import swgohManager.model.Joueur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JoueurRepository extends JpaRepository<Joueur, Long> {

    Optional<Joueur> findByPlayerId(String playerId);
    

    List<Joueur> findAllByPresentInGuildTrue();
}